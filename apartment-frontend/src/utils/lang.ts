
export function translateField(fieldValue: any, locale: string): string {
  if (!fieldValue) return '';
  if (typeof fieldValue === 'object') {
    return fieldValue[locale] || fieldValue['zh'] || JSON.stringify(fieldValue);
  }
  try {
    const parsed = JSON.parse(fieldValue);
    if (typeof parsed === 'object' && parsed !== null) {
      return parsed[locale] || parsed['zh'] || fieldValue;
    }
  } catch (e) {
    // Not a JSON string
  }
  return fieldValue;
}
