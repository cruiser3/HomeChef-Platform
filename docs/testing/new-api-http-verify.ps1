$ErrorActionPreference="Continue"
$Base="http://localhost:8080"; $global:Pass=0; $global:Fail=0
function Check($n,$c){ if($c){$global:Pass++;Write-Host "PASS $n"}else{$global:Fail++;Write-Host "FAIL $n"} }
function Req($m,$path,$body,$tok,$fid){
  $p=@{Uri="$Base$path";Method=$m;UseBasicParsing=$true}; $h=@{}
  if($tok){$h["Authorization"]="Bearer $tok"}; if($fid){$h["X-Family-Id"]="$fid"}
  if($h.Count){$p.Headers=$h}; if($body){$p.ContentType="application/json";$p.Body=$body}
  try{$r=Invoke-WebRequest @p;return @{Status=[int]$r.StatusCode;Body=$r.Content}}
  catch [System.Net.WebException]{$w=$_.Exception.Response;if($w){$s=[int]$w.StatusCode;$sr=New-Object System.IO.StreamReader($w.GetResponseStream());return @{Status=$s;Body=$sr.ReadToEnd()}};return @{Status=-1;Body=$_.Exception.Message}}
  catch{return @{Status=-1;Body=$_.Exception.Message}}
}
function J($r){try{return $r.Body|ConvertFrom-Json}catch{return $null}}
function E($n,$r,$c){Check "$n -> $($r.Status)" ($r.Status -eq $c)}

$s=Get-Date -Format "HHmmss"
$null=Req "Post" "/api/auth/register" (@{name="Alice";email="alice$s@t.test";password="123456"}|ConvertTo-Json) $null $null
$null=Req "Post" "/api/auth/register" (@{name="Bob";email="bob$s@t.test";password="123456"}|ConvertTo-Json) $null $null
$null=Req "Post" "/api/auth/register" (@{name="Eve";email="eve$s@t.test";password="123456"}|ConvertTo-Json) $null $null
$A=J (Req "Post" "/api/auth/login" (@{email="alice$s@t.test";password="123456"}|ConvertTo-Json) $null $null)
$B=J (Req "Post" "/api/auth/login" (@{email="bob$s@t.test";password="123456"}|ConvertTo-Json) $null $null)
$E=J (Req "Post" "/api/auth/login" (@{email="eve$s@t.test";password="123456"}|ConvertTo-Json) $null $null)
$ta=$A.token;$tb=$B.token;$te=$E.token
Check "tokens present" ($ta -and $tb -and $te)

$fa=J (Req "Post" "/api/families" (@{name="FA$s"}|ConvertTo-Json) $ta $null); $faId=$fa.id
$fe=J (Req "Post" "/api/families" (@{name="FE$s"}|ConvertTo-Json) $te $null); $feId=$fe.id
Check "families created" ($faId -and $feId)

Write-Host "--- invite code ---"
$c1=J (Req "Get" "/api/families/$faId/invite-code" $null $ta $null)
$c2=J (Req "Post" "/api/families/$faId/invite-code/reset" $null $ta $null)
Check "invite code returned" ($c1.inviteCode -and $c2.inviteCode)
Check "reset changes code" ($c1.inviteCode -ne $c2.inviteCode)
E "bob join" (Req "Post" "/api/families/join" (@{inviteCode=$c2.inviteCode}|ConvertTo-Json) $tb $null) 200
E "join bad code -> 400" (Req "Post" "/api/families/join" (@{inviteCode="ZZZZZZZZ"}|ConvertTo-Json) $tb $null) 400
E "join again -> 409" (Req "Post" "/api/families/join" (@{inviteCode=$c2.inviteCode}|ConvertTo-Json) $tb $null) 409

Write-Host "--- M-09 profile ---"
$m=J (Req "Get" "/api/users/me" $null $ta $null)
Check "me name=Alice" ($m.name -eq "Alice")
$u=J (Req "Put" "/api/users/me" (@{name="AliceR";avatarUrl="https://x/a.png";dietaryPreferences=@("vegetarian","fitness")}|ConvertTo-Json) $ta $null)
Check "update name" ($u.name -eq "AliceR")
Check "update dietary upper+sorted" (($u.dietaryPreferences -join ",") -eq "FITNESS,VEGETARIAN")
E "bad pref -> 400" (Req "Put" "/api/users/me" (@{name="AliceR";dietaryPreferences=@("KETO")}|ConvertTo-Json) $ta $null) 400
E "blank name -> 400" (Req "Put" "/api/users/me" (@{name="  "}|ConvertTo-Json) $ta $null) 400
E "6 prefs -> 400" (Req "Put" "/api/users/me" (@{name="AliceR";dietaryPreferences=@("FITNESS","FAT_LOSS","MUSCLE_GAIN","LIGHT_DIET","VEGETARIAN","X")}|ConvertTo-Json) $ta $null) 400
$m2=J (Req "Get" "/api/users/me" $null $ta $null)
Check "profile persisted" (($m2.dietaryPreferences -join ",") -eq "FITNESS,VEGETARIAN")

Write-Host "--- member remove / role ---"
$mem=J (Req "Get" "/api/families/$faId/members" $null $ta $null)
$aliceId=($mem|Where-Object{$_.role -eq "ADMIN"}|Select-Object -First 1).userId
$bobId=($mem|Where-Object{$_.role -eq "MEMBER"}|Select-Object -First 1).userId
Check "member ids found" ($aliceId -and $bobId)
E "bob reset invite -> 403" (Req "Post" "/api/families/$faId/invite-code/reset" $null $tb $null) 403
E "bob change role -> 403" (Req "Put" "/api/families/$faId/members/$aliceId/role" (@{role="MEMBER"}|ConvertTo-Json) $tb $null) 403
E "bob remove member -> 403" (Req "Delete" "/api/families/$faId/members/$aliceId" $null $tb $null) 403
$r1=J (Req "Put" "/api/families/$faId/members/$bobId/role" (@{role="ADMIN"}|ConvertTo-Json) $ta $null)
Check "alice promote bob -> ADMIN" ($r1.role -eq "ADMIN")
$r2=J (Req "Put" "/api/families/$faId/members/$bobId/role" (@{role="MEMBER"}|ConvertTo-Json) $ta $null)
Check "alice demote bob -> MEMBER" ($r2.role -eq "MEMBER")
E "invalid role -> 400" (Req "Put" "/api/families/$faId/members/$bobId/role" (@{role="OWNER"}|ConvertTo-Json) $ta $null) 400
E "role for missing user -> 404" (Req "Put" "/api/families/$faId/members/999999/role" (@{role="MEMBER"}|ConvertTo-Json) $ta $null) 404
E "remove missing user -> 404" (Req "Delete" "/api/families/$faId/members/999999" $null $ta $null) 404
E "remove self -> 400" (Req "Delete" "/api/families/$faId/members/$aliceId" $null $ta $null) 400
E "demote last admin -> 400" (Req "Put" "/api/families/$faId/members/$aliceId/role" (@{role="MEMBER"}|ConvertTo-Json) $ta $null) 400

Write-Host "--- shopping list edit/purchase/delete ---"
$it=J (Req "Post" "/api/shopping-list/items" (@{name="ginger";amount=50;unit="g"}|ConvertTo-Json) $ta $faId)
$itId=$it.id
Check "shopping item created" ($itId -ne $null)
$up=J (Req "Put" "/api/shopping-list/items/$itId" (@{name="garlic";amount=100;unit="g"}|ConvertTo-Json) $ta $faId)
Check "edit item name" ($up.name -eq "garlic")
Check "edit item amount" ([decimal]$up.amount -eq 100)
$p1=J (Req "Patch" "/api/shopping-list/items/$itId/purchased" (@{purchased=$true}|ConvertTo-Json) $ta $faId)
Check "mark purchased" ($p1.purchased -eq $true)
$p2=J (Req "Patch" "/api/shopping-list/items/$itId/purchased" (@{purchased=$false}|ConvertTo-Json) $ta $faId)
Check "mark unpurchased" ($p2.purchased -eq $false)
E "delete item -> 204" (Req "Delete" "/api/shopping-list/items/$itId" $null $ta $faId) 204
E "delete again -> 404" (Req "Delete" "/api/shopping-list/items/$itId" $null $ta $faId) 404
$it2=J (Req "Post" "/api/shopping-list/items" (@{name="salt";amount=10;unit="g"}|ConvertTo-Json) $ta $faId)
E "cross-family delete -> 404" (Req "Delete" "/api/shopping-list/items/$($it2.id)" $null $te $feId) 404

Write-Host "--- dish delete ---"
$dx=J (Req "Post" "/api/dishes" (@{name="DishX$s";price=20.00}|ConvertTo-Json) $ta $faId)
Check "dish X created" ($dx.id -ne $null)
E "bob delete dish -> 403" (Req "Delete" "/api/dishes/$($dx.id)" $null $tb $faId) 403
E "alice delete dish -> 204" (Req "Delete" "/api/dishes/$($dx.id)" $null $ta $faId) 204
E "deleted dish detail -> 404" (Req "Get" "/api/dishes/$($dx.id)" $null $ta $faId) 404
$dy=J (Req "Post" "/api/dishes" (@{name="DishY$s";price=15.00}|ConvertTo-Json) $ta $faId)
$null=Req "Post" "/api/cart/items" (@{dishId=$dy.id;quantity=1}|ConvertTo-Json) $ta $faId
$ord=Req "Post" "/api/family-orders" (@{mealPeriod="LUNCH"}|ConvertTo-Json) $ta $faId
Check "order referencing dish created" ($ord.Status -eq 201)
E "delete referenced dish -> 409" (Req "Delete" "/api/dishes/$($dy.id)" $null $ta $faId) 409

Write-Host ""
Write-Host "=== RESULT: PASS=$global:Pass FAIL=$global:Fail ==="
