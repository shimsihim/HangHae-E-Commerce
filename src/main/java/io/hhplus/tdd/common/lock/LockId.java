package io.hhplus.tdd.common.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lock을 적용할 ID 필드를 표시하는 어노테이션
 * Record의 컴포넌트(필드)에 적용하여 해당 값을 락의 키로 사용합니다.
 *
 * 사용 예시:
 * <pre>
 * public record Input(
 *     @LockId long userId,
 *     long amount
 * ) {}
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LockId {
}
