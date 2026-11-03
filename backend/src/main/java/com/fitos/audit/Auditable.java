package com.fitos.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Task #33: đánh dấu method cần ghi audit log. Aspect sẽ ghi actor, hành động, entity,
 * entityId, tham số và IP; lỗi audit không được làm hỏng nghiệp vụ.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Hành động, ví dụ CREATE_ORDER, PAYMENT_CALLBACK, RENEW_MEMBERSHIP. */
    String action();

    /** Tên entity bị tác động, ví dụ Order, Payment, Membership. */
    String entity();
}
