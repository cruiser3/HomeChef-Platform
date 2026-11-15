# =============================================================================
# REBUILT VERSION - NOT THE ORIGINAL SCRIPT
# Original file: target/order-http-verify.ps1 (LOST - target/ was cleaned and the
# file was never tracked by Git; `git ls-files "*.ps1"` and `git log --all -- "*.ps1"`
# both return nothing).
#
# Rebuilt on: 2026-11-12
# Rebuilt from:
#   1. docs/backend/ORDER_MODULE.md  section 10.2 (assertion table A20-A85)
#   2. order-http-result.txt (real captured run: "=== RESULT: PASS=85 FAIL=0 ===")
# The assertion names and expected values below are copied from that recorded run.
# Rebuild verified on 2026-11-13 against a running instance (Spring Boot 3.3.2, H2):
#   === RESULT: PASS=63 FAIL=0 ===
# Note: the original run recorded 85 assertions; this rebuild covers 63 of them
# (the core order / cook-status / isolation / shopping-list / availability flows).
# The original 85-assertion coverage can NOT be fully reconstructed from the
# remaining docs, so this file must not be presented as equivalent to the original.
#
# Prerequisite: backend running on http://localhost:8080
# Usage:  powershell -ExecutionPolicy Bypass -File docs\testing\order-http-verify-rebuilt.ps1
# =============================================================================

$ErrorActionPreference = "Continue"
$Base = "http://localhost:8080"
$Pass = 0
$Fail = 0

function Check($name, $cond) {
    if ($cond) { $script:Pass++; Write-Host "PASS  $name" }
    else { $script:Fail++; Write-Host "FAIL  $name" }
}

function Req($method, $path, $body, $token, $familyId) {
    $p = @{ Uri = "$Base$path"; Method = $method; UseBasicParsing = $true }
    $h = @{}
    if ($token)    { $h["Authorization"] = "Bearer $token" }
    if ($familyId) { $h["X-Family-Id"] = "$familyId" }
    if ($h.Count -gt 0) { $p.Headers = $h }
    if ($body) { $p.ContentType = "application/json"; $p.Body = $body }
    try {
        $resp = Invoke-WebRequest @p
        return @{ Status = [int]$resp.StatusCode; Body = $resp.Content }
    } catch [System.Net.WebException] {
        $web = $_.Exception.Response
        if ($web -ne $null) {
            $code = [int]$web.StatusCode
            $sr = New-Object System.IO.StreamReader($web.GetResponseStream())
            return @{ Status = $code; Body = $sr.ReadToEnd() }
        }
        return @{ Status = -1; Body = $_.Exception.Message }
    } catch {
        return @{ Status = -1; Body = $_.Exception.Message }
    }
}

function J($r) { try { return ($r.Body | ConvertFrom-Json) } catch { return $null } }

function JC($r) {
    $v = J $r
    if ($null -eq $v) { return 0 }
    if ($v -is [array]) { return $v.Count }
    return 1
}

function Expect($name, $r, $code) { Check "$name -> $($r.Status)" ($r.Status -eq $code) }

Write-Host "=== 0. register users ==="
$stamp = Get-Date -Format "HHmmss"
$null = Req "Post" "/api/auth/register" "{`"name`":`"Alice`",`"email`":`"alice$stamp@family.test`",`"password`":`"123456`"}" $null $null
$null = Req "Post" "/api/auth/register" "{`"name`":`"Bob`",`"email`":`"bob$stamp@family.test`",`"password`":`"123456`"}" $null $null
$null = Req "Post" "/api/auth/register" "{`"name`":`"Eve`",`"email`":`"eve$stamp@family.test`",`"password`":`"123456`"}" $null $null

Write-Host "=== 1. login ==="
$alice = J (Req "Post" "/api/auth/login" "{`"email`":`"alice$stamp@family.test`",`"password`":`"123456`"}" $null $null)
$bob   = J (Req "Post" "/api/auth/login" "{`"email`":`"bob$stamp@family.test`",`"password`":`"123456`"}" $null $null)
$eve   = J (Req "Post" "/api/auth/login" "{`"email`":`"eve$stamp@family.test`",`"password`":`"123456`"}" $null $null)
$tokenA = $alice.token; $tokenB = $bob.token; $tokenE = $eve.token
Check "alice token present" ($null -ne $tokenA)
Check "bob token present"   ($null -ne $tokenB)
Check "eve token present"   ($null -ne $tokenE)

Write-Host "=== 2. create families ==="
$fa = J (Req "Post" "/api/families" "{`"name`":`"Family A $stamp`"}" $tokenA $null)
$fb = J (Req "Post" "/api/families" "{`"name`":`"Family B $stamp`"}" $tokenB $null)
$fe = J (Req "Post" "/api/families" "{`"name`":`"Eve Family $stamp`"}" $tokenE $null)
$faId = $fa.id; $fbId = $fb.id; $feId = $fe.id
Check "family A created"   ($null -ne $faId)
Check "family B created"   ($null -ne $fbId)
Check "eve family created" ($null -ne $feId)

Write-Host "=== 3. create dishes ==="
$pork = J (Req "Post" "/api/dishes" "{`"name`":`"Pork`",`"price`":38.00,`"available`":true}" $tokenA $faId)
$cuke = J (Req "Post" "/api/dishes" "{`"name`":`"Cucumber`",`"price`":12.00,`"available`":true}" $tokenA $faId)
$fbD  = J (Req "Post" "/api/dishes" "{`"name`":`"FamilyB Dish`",`"price`":20.00,`"available`":true}" $tokenB $fbId)
$porkId = $pork.id; $cukeId = $cuke.id; $fbDishId = $fbD.id
Check "pork dish created"     ($null -ne $porkId)
Check "cucumber dish created" ($null -ne $cukeId)
Check "familyB dish created"  ($null -ne $fbDishId)

Write-Host "=== 4. cart ==="
Expect "add pork x2" (Req "Post" "/api/cart/items" "{`"dishId`":$porkId,`"quantity`":2}" $tokenA $faId) 200
$cart1 = J (Req "Get" "/api/cart" $null $tokenA $faId)
Check "cart total 76.00" ([decimal]$cart1.totalAmount -eq 76.00)
Expect "add cucumber x1" (Req "Post" "/api/cart/items" "{`"dishId`":$cukeId,`"quantity`":1}" $tokenA $faId) 200
$cart2 = J (Req "Get" "/api/cart" $null $tokenA $faId)
Check "cart total 88.00" ([decimal]$cart2.totalAmount -eq 88.00)

Write-Host "=== 5. create order ==="
$order = J (Req "Post" "/api/family-orders" "{`"mealPeriod`":`"DINNER`"}" $tokenA $faId)
Check "order status CREATED"     ($order.status -eq "CREATED")
Check "order total 88.00"        ([decimal]$order.totalAmount -eq 88.00)
Check "order itemCount 2"        ($order.itemCount -eq 2)
Check "order mealPeriod DINNER"  ($order.mealPeriod -eq "DINNER")
Check "order mealServed false"   ($order.mealServed -eq $false)
Check "order items length 2"     ($order.items.Count -eq 2)
Check "item0 cook status PENDING" ($order.items[0].cookStatus -eq "PENDING")
Check "item0 name snapshot Pork"  ($order.items[0].dishName -eq "Pork")
Check "item0 unit price 38.00"    ([decimal]$order.items[0].unitPrice -eq 38.00)
Check "item0 subtotal 76.00"      ([decimal]$order.items[0].subtotal -eq 76.00)
Check "item1 subtotal 12.00"      ([decimal]$order.items[1].subtotal -eq 12.00)
$orderId = $order.id

Write-Host "=== 6. cart cleared after order ==="
$cart3 = J (Req "Get" "/api/cart" $null $tokenA $faId)
Check "cart itemCount 0"  ($cart3.itemCount -eq 0)
Check "cart total zero"   ([decimal]$cart3.totalAmount -eq 0)

Write-Host "=== 7. empty cart rejected ==="
Expect "empty cart rejected" (Req "Post" "/api/family-orders" "{`"mealPeriod`":`"LUNCH`"}" $tokenA $faId) 400

Write-Host "=== 8. order detail ==="
$detail = J (Req "Get" "/api/family-orders/$orderId" $null $tokenA $faId)
Check "detail total 88.00" ([decimal]$detail.totalAmount -eq 88.00)
Check "detail items 2"     ($detail.items.Count -eq 2)

Write-Host "=== 9. per-dish cook status ==="
$i0 = $detail.items[0].id
$i1 = $detail.items[1].id
Expect "item0 to COOKING" (Req "Patch" "/api/family-orders/items/$i0/cook-status" "{`"status`":`"COOKING`"}" $tokenA $faId) 200
Expect "item0 to READY"   (Req "Patch" "/api/family-orders/items/$i0/cook-status" "{`"status`":`"READY`"}"   $tokenA $faId) 200
Expect "item1 to BLOCKED" (Req "Patch" "/api/family-orders/items/$i1/cook-status" "{`"status`":`"BLOCKED`",`"blockedReason`":`"no gas`"}" $tokenA $faId) 200
Expect "illegal BLOCKED->SERVED rejected" (Req "Patch" "/api/family-orders/items/$i1/cook-status" "{`"status`":`"SERVED`"}"  $tokenA $faId) 400
Expect "illegal READY->COOKING rejected"  (Req "Patch" "/api/family-orders/items/$i0/cook-status" "{`"status`":`"COOKING`"}" $tokenA $faId) 400
Expect "unknown cook status rejected"     (Req "Patch" "/api/family-orders/items/$i0/cook-status" "{`"status`":`"NOPE`"}"    $tokenA $faId) 400
Expect "BLOCKED without reason rejected"  (Req "Patch" "/api/family-orders/items/$i1/cook-status" "{`"status`":`"BLOCKED`"}" $tokenA $faId) 400

Write-Host "=== 10. start preparing / meal served ==="
Expect "start preparing" (Req "Post" "/api/family-orders/$orderId/start-preparing" "{}" $tokenA $faId) 200
$after = J (Req "Get" "/api/family-orders/$orderId" $null $tokenA $faId)
Check "order status PREPARING"      ($after.status -eq "PREPARING")
Check "item0 cook status unchanged" ($after.items[0].cookStatus -eq "READY")
Expect "meal served" (Req "Post" "/api/family-orders/$orderId/meal-served" "{}" $tokenA $faId) 200
$served = J (Req "Get" "/api/family-orders/$orderId" $null $tokenA $faId)
Check "mealServed true"       ($served.mealServed -eq $true)
Check "mealServedAt present"  ($null -ne $served.mealServedAt)
Check "order still PREPARING" ($served.status -eq "PREPARING")

Write-Host "=== 11. assign responsible must be family member ==="
Expect "assign non-member rejected" (Req "Patch" "/api/family-orders/items/$i1/responsible" "{`"responsibleUserId`":999999}" $tokenA $faId) 403
$members = J (Req "Get" "/api/families/$faId/members" $null $tokenA $null)
$aliceMemberId = $members[0].userId
Check "alice member id found" ($null -ne $aliceMemberId)
Expect "assign alice" (Req "Patch" "/api/family-orders/items/$i1/responsible" "{`"responsibleUserId`":$aliceMemberId}" $tokenA $faId) 200

Write-Host "=== 12. order list ==="
$list = J (Req "Get" "/api/family-orders" $null $tokenA $faId)
Check "list has 1 order" ((JC (Req "Get" "/api/family-orders" $null $tokenA $faId)) -eq 1)
Write-Host "=== 13. isolation ==="
Expect "eve order detail 403" (Req "Get" "/api/family-orders/$orderId" $null $tokenE $faId) 403
Expect "familyB reads familyA order 404" (Req "Get" "/api/family-orders/$orderId" $null $tokenB $fbId) 404

Write-Host "=== 14. shopping list ==="
Expect "add shopping item" (Req "Post" "/api/shopping-list/items" "{`"name`":`"ginger`",`"amount`":50,`"unit`":`"g`",`"orderId`":$orderId}" $tokenA $faId) 201
$sb = J (Req "Get" "/api/shopping-list" $null $tokenA $faId)
Check "shopping list has 1 item" ((JC (Req "Get" "/api/shopping-list" $null $tokenA $faId)) -eq 1)
Expect "eve shopping list familyA 403" (Req "Get" "/api/shopping-list" $null $tokenE $faId) 403
Expect "cross-family order link rejected" (Req "Post" "/api/shopping-list/items" "{`"name`":`"garlic`",`"amount`":30,`"unit`":`"g`",`"orderId`":$orderId}" $tokenB $fbId) 400

Write-Host "=== 15. unavailable dish cannot be ordered ==="
Expect "disable cucumber" (Req "Patch" "/api/dishes/$cukeId/availability?available=false" $null $tokenA $faId) 200
Expect "add unavailable dish rejected" (Req "Post" "/api/cart/items" "{`"dishId`":$cukeId,`"quantity`":1}" $tokenA $faId) 400
Expect "add available dish" (Req "Post" "/api/cart/items" "{`"dishId`":$porkId,`"quantity`":1}" $tokenA $faId) 200
Expect "disable pork" (Req "Patch" "/api/dishes/$porkId/availability?available=false" $null $tokenA $faId) 200
Expect "order with unavailable dish rejected" (Req "Post" "/api/family-orders" "{`"mealPeriod`":`"LUNCH`"}" $tokenA $faId) 400
$keep = J (Req "Get" "/api/cart" $null $tokenA $faId)
Check "cart preserved after failed order" ($keep.itemCount -eq 1)
Expect "enable pork" (Req "Patch" "/api/dishes/$porkId/availability?available=true" $null $tokenA $faId) 200

Write-Host "=== 16. successful order clears only own cart ==="
$beforeCount = JC (Req "Get" "/api/family-orders" $null $tokenA $faId)
Expect "order after re-enable" (Req "Post" "/api/family-orders" "{`"mealPeriod`":`"LUNCH`"}" $tokenA $faId) 201
$finalCart = J (Req "Get" "/api/cart" $null $tokenA $faId)
Check "alice familyA cart cleared" ($finalCart.itemCount -eq 0)
Check "familyA order count increased by 1" ((JC (Req "Get" "/api/family-orders" $null $tokenA $faId)) -eq ($beforeCount + 1))

Write-Host ""
Write-Host "=== RESULT: PASS=$Pass FAIL=$Fail ==="