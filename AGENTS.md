# Project Constraints

This file defines coding conventions and architectural constraints for the gensis-apartment project.

## Error Handling Convention

### Backend (Java Spring Boot)

**ALL backend errors MUST use error codes.**

1. Define error codes in `ErrorCode.java`:

```java
public enum ErrorCode {
    // Pattern: DOMAIN_SPECIFIC_ERROR
    ORDER_ROOM_EMPTY("order.ROOM_EMPTY"),
    ORDER_NOT_FOUND("order.NOT_FOUND"),
    // ...
}
```

2. Throw `BusinessException` with error code (and optional args):

```java
// Correct
throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);

// With arguments for message interpolation
throw new BusinessException(ErrorCode.ORDER_ROOM_TIME_CONFLICT, roomNo);

// WRONG - Never throw raw messages
throw new BusinessException("订单未找到");  // ❌ FORBIDDEN
throw new RuntimeException("Something went wrong");  // ❌ FORBIDDEN
```

3. Global exception handler returns structured response:

```json
{
  "code": "order.NOT_FOUND",
  "args": []
}
```

### Frontend (Vue 3)

**ALL frontend errors MUST be translated using error codes.**

1. Define translations in `errorTranslate.ts`:

```typescript
const errorMessagesZh: Record<string, string> = {
  'order.NOT_FOUND': '订单未找到',
  'order.ROOM_TIME_CONFLICT': '房间 {0} 的入住时间冲突！',
  // ...
};
```

2. Use translation functions:

```typescript
// Admin backend (always Chinese)
import { getErrorMessageZh } from '@/utils/errorTranslate';
alert('操作失败: ' + getErrorMessageZh(error));

// Mobile app (i18n-aware)
import { getErrorMessageI18n } from '@/utils/errorTranslate';
alert(getErrorMessageI18n(error));
```

3. **Admin panel ALWAYS uses Chinese (`getErrorMessageZh`).**

### Error Code Naming Convention

- Format: `{DOMAIN}_{SPECIFIC_ERROR}`
- Use UPPERCASE with underscores
- Domain examples: `ORDER`, `AUTH`, `MAINT`, `NOTIF`, `GENERAL`
- Examples:
  - `order.ROOM_EMPTY` - Room field is empty
  - `order.NOT_FOUND` - Order not found
  - `auth.USER_NOT_FOUND` - User not found
  - `maint.ORDER_CONFLICT` - Maintenance conflicts with order

### Adding New Errors

1. Add code to `ErrorCode.java`
2. Add Chinese translation to `errorTranslate.ts` (required)
3. Add English translation to i18n locale files if needed
4. Throw `BusinessException` with the code in service layer

### Error Categories

1. **Business Errors** (user-facing, recoverable) → MUST use `BusinessException` with `ErrorCode`
   - Validation failures
   - Business rule violations
   - Resource not found
   - Conflict/constraint violations

2. **System Errors** (infrastructure, non-recoverable) → May use `RuntimeException`
   - External API failures (WeChat Work, email service)
   - File/Excel generation failures
   - Database connection issues
   - Unexpected infrastructure errors

3. **Parameter Validation** → Use `IllegalArgumentException` only for programming errors (null checks in internal methods). For user-facing parameter errors, use `BusinessException`.

### Anti-Patterns (FORBIDDEN)

```java
// ❌ Business error with raw string
throw new RuntimeException("订单未找到");  // Should use BusinessException

// ❌ Direct message in BusinessException
throw new BusinessException(ErrorCode.PARAM_ERROR, "自定义消息");  // Use args instead

// ❌ Missing error code in response
return ResponseEntity.badRequest().body("操作失败");
```

```typescript
// ❌ Showing raw error message
alert(error.response?.data?.message || '操作失败');

// ❌ Hardcoded error text in catch block
catch (e) {
  alert('发送失败');  // Should use getErrorMessageZh(e)
}
```

## Other Conventions

### API Response Format

Success:
```json
{
  "data": { ... }
}
```

Error:
```json
{
  "code": "domain.ERROR_CODE",
  "args": ["arg1", "arg2"]
}
```

### Entity Relationships

- Use `@OneToMany` with `cascade = CascadeType.ALL, orphanRemoval = true` for owned entities
- Always add `@JsonIgnore` on bidirectional `@ManyToOne` back-references to prevent circular serialization

### Transaction Management

- Use `@Transactional` on service methods that modify data
- Read-only operations don't need `@Transactional`
