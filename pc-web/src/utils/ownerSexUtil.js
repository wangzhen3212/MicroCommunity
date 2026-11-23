export function ownerSexKey(sex) {
  const val = sex === null || sex === undefined ? '' : String(sex).trim()
  if (val === '0' || val === 'M' || val === 'm' || val === '男') return 'male'
  if (val === '1' || val === 'F' || val === 'f' || val === '女') return 'female'
  return ''
}

export function formatOwnerSex(sex, t) {
  const key = ownerSexKey(sex)
  return key ? t('common.' + key) : '-'
}
