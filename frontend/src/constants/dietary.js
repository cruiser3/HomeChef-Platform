// 饮食偏好与忌口标签的共享常量（白名单，与后端枚举一一对应）
export const PREFERENCES = [
  { value: 'FITNESS', label: '健身' },
  { value: 'FAT_LOSS', label: '减脂' },
  { value: 'MUSCLE_GAIN', label: '增肌' },
  { value: 'LIGHT_DIET', label: '清淡' },
  { value: 'VEGETARIAN', label: '素食' },
]

export const RESTRICTIONS = [
  { value: 'NO_SPICY', label: '不吃辣' },
  { value: 'NO_CILANTRO', label: '不吃香菜' },
  { value: 'NO_SEAFOOD', label: '不吃海鲜' },
  { value: 'NO_BEEF_MUTTON', label: '不吃牛羊肉' },
  { value: 'NO_PORK', label: '不吃猪肉' },
  { value: 'NO_ALLIUM', label: '不吃葱姜蒜' },
]

export function restrictionLabel(value) {
  return RESTRICTIONS.find((r) => r.value === value)?.label || value
}

export function restrictionsToText(values) {
  if (!values || values.length === 0) return ''
  return '忌口：' + values.map(restrictionLabel).join('、')
}

export const ALLERGIES = [
  { value: 'SEAFOOD', label: '海鲜' },
  { value: 'PEANUT', label: '花生' },
  { value: 'TREE_NUT', label: '坚果' },
  { value: 'MILK', label: '牛奶' },
  { value: 'EGG', label: '鸡蛋' },
  { value: 'SOY', label: '大豆' },
  { value: 'WHEAT', label: '小麦(麸质)' },
  { value: 'SESAME', label: '芝麻' },
]

export function allergyLabel(value) {
  return ALLERGIES.find((a) => a.value === value)?.label || value
}

export function allergiesToText(values) {
  if (!values || values.length === 0) return ''
  return '过敏：' + values.map(allergyLabel).join('、')
}

// 疾病 / 健康情况标签（敏感健康信息，仅用于饮食参考，不构成医疗建议）
export const CONDITIONS = [
  { value: 'DIABETES', label: '糖尿病' },
  { value: 'HYPERTENSION', label: '高血压' },
  { value: 'HYPERLIPIDEMIA', label: '高血脂' },
  { value: 'GOUT', label: '痛风' },
  { value: 'KIDNEY_DISEASE', label: '肾病' },
  { value: 'FATTY_LIVER', label: '脂肪肝' },
  { value: 'CORONARY_HEART_DISEASE', label: '冠心病' },
  { value: 'GASTRITIS', label: '胃炎' },
]

export function conditionLabel(value) {
  return CONDITIONS.find((c) => c.value === value)?.label || value
}

export function conditionsToText(values) {
  if (!values || values.length === 0) return ''
  return '健康情况：' + values.map(conditionLabel).join('、')
}
