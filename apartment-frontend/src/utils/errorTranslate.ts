import { i18n } from '../main';

// 错误码翻译映射 - 后台管理直接显示中文
const errorMessagesZh: Record<string, string> = {
  // Auth
  'auth.CODE_FORMAT_INVALID': '验证码格式错误，请输入4-6位数字',
  'auth.PHONE_NOT_REGISTERED': '手机号未注册，请通过企业微信登录',
  'auth.PHONE_IN_USE': '该手机号已被其他用户使用',
  'auth.USER_NOT_FOUND': '用户不存在',
  'user.EMAIL_REQUIRED': '人工添加的用户邮箱为必填项',

  // Order
  'order.ROOM_EMPTY': '房间不能为空',
  'order.DATES_EMPTY': '入住和离开日期不能为空',
  'order.END_BEFORE_START': '离开时间必须晚于入住时间',
  'order.ROOM_NO_EMPTY': '房号不能为空',
  'order.ROOM_TIME_CONFLICT': '房间 {0} 的入住到离开时间与已有的未结订单冲突！',
  'order.ROOM_MAINTENANCE': '房间 {0} 处于维修状态！',
  'order.OCCUPANT_CONFLICT': '入住人 {0} 在此期间已有其他房间预约，存在时段冲突！',
  'order.INDIVIDUAL_ONE_CURRENT': '个人客人订单只允许一条记录为当前状态',
  'order.NOT_FOUND': '订单未找到',
  'order.NO_ROOM_RECORDS': '该订单没有房间记录',
  'order.ROOM_CARD_EMPTY': '房卡号不能为空',
  'order.DOOR_CODE_EMPTY': '门锁密码不能为空',
  'order.CANCEL_DEADLINE_PASSED': '取消预订须于入住日前1天24:00前操作，现已超过截止时间',
  'order.ALREADY_EXISTS': '订单已存在，请确认订单',
  'order.OCCUPY_NOT_FOUND': '入住记录未找到',
  'order.TIME_CONFLICT': '该时段与房间已有的订单冲突',
  'order.MAINTENANCE_CONFLICT': '该时段房间处于维修状态',
  'order.OCCUPANT_TIME_CONFLICT': '该入住人在所选时段已有其他预约，存在冲突',
  'order.KEY_BOX_NO_EMPTY': '房卡箱号不能为空',
  'order.BOX_PASSWORD_EMPTY': '箱密码不能为空',

  // Maintenance
  'maint.START_AFTER_END': '开始时间不能晚于结束时间',
  'maint.ORDER_CONFLICT': '所选时间段内房间已有预订订单，冲突！',
  'maint.OTHER_CONFLICT': '所选时间段内房间已有其他维修记录，冲突！',
  'maint.COMPLETED_NO_REVERT': '已完成的维修记录只能作废，不能改回维修中',
  'maint.COMPLETED_NO_DELETE': '已完成的维修记录不允许删除',
  'maint.ACTIVE_NO_DELETE': '维修中的记录不允许删除，只能取消',

  // Notification
  'notif.RECIPIENT_MISSING': '接收人信息缺失',
  'notif.UNKNOWN_CHANNEL': '未知通知渠道',

  // General
  'general.EXCEL_FAILED': 'Excel生成失败',
  'general.ORDER_NOT_FOUND': '订单未找到',
  'general.ROOM_NOT_FOUND': '房间未找到',
  'general.OCCUPY_NOT_FOUND': '入住记录未找到',
  'general.DATA_CONSTRAINT_VIOLATION': '数据约束冲突或无效请求',
  'general.INTERNAL_ERROR': '服务器内部错误',
  'runtime.UNEXPECTED': '操作失败'
};

/**
 * 翻译后端返回的错误信息
 * @param error 响应错误对象，格式为 { code: string, args?: any[], message?: string }
 * @param locale 语言，默认中文
 */
export function translateError(error: any, locale: string = 'zh'): string {
  const code = error?.code;
  const args = error?.args || [];
  const fallbackMessage = error?.message || '操作失败';

  if (!code) {
    return fallbackMessage;
  }

  // 根据语言获取翻译
  let message: string;

  if (locale === 'zh') {
    message = errorMessagesZh[code];
  } else {
    // 使用 i18n 进行翻译
    try {
      const t = i18n.global.t;
      message = t(`errors.${code}`);
    } catch {
      message = errorMessagesZh[code]; // 回退到中文
    }
  }

  if (!message) {
    return fallbackMessage;
  }

  // 替换参数占位符 {0}, {1}, ...
  if (args && args.length > 0) {
    args.forEach((arg: any, index: number) => {
      message = message.replace(new RegExp(`\\{${index}\\}`, 'g'), String(arg));
    });
  }

  return message;
}

/**
 * 从 Axios 错误响应中提取并翻译错误信息
 * 用于后台管理界面（直接显示中文）
 */
export function getErrorMessageZh(error: any): string {
  const errorData = error?.response?.data;
  if (errorData && typeof errorData === 'object') {
    return translateError(errorData, 'zh');
  }
  return error?.message || '操作失败';
}

/**
 * 从 Axios 错误响应中提取并翻译错误信息
 * 用于移动端界面（根据当前语言显示）
 */
export function getErrorMessageI18n(error: any): string {
  const errorData = error?.response?.data;
  const locale = localStorage.getItem('locale') || 'zh';

  if (errorData && typeof errorData === 'object') {
    return translateError(errorData, locale);
  }
  return error?.message || '操作失败';
}
