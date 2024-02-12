package hello.aop.order.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order

private val logger = LoggerFactory.getLogger(AspectV5Order::class.java)

class AspectV5Order {

    @Aspect
    @Order(2)
    class LogAspect {
        @Around("hello.aop.order.aop.Pointcuts.allOrder()") // 포인트컷
        fun doLog(joinPoint: ProceedingJoinPoint): Any? { // 어드바이스
            logger.info("[log] ${joinPoint.signature}")
            return joinPoint.proceed()
        }
    }

    @Aspect
    @Order(1)
    class TxAspect {
        // hello.aop.order 패키지와 하위 패키지 이면서 클래스 이름 패턴이 *Service
        @Around("hello.aop.order.aop.Pointcuts.orderAndService()") // 포인트컷
        fun doTransaction(joinPoint: ProceedingJoinPoint): Any? {
            return try {
                logger.info("[트랜잭션 시작] ${joinPoint.signature}")
                val result = joinPoint.proceed()
                logger.info("[트랜잭션 커밋] ${joinPoint.signature}")
                result
            } catch (e: Exception) {
                logger.info("[트랜잭션 롤백] ${joinPoint.signature}")
                throw e
            } finally {
                logger.info("[리소스 릴리즈] ${joinPoint.signature}")
            }
        }
    }
}