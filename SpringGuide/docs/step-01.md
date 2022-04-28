

# 통일된 Error Response 객체

- Error Response 객체는 항상 동일한 Error Response를 가져야 한다.
- 그렇지 않으면 클라인트에서 예외 처리를 항상 동일한 로직으로 처리하기 어려움

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    log.error("handleMethodArgumentNotValidException", e);
    final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
}
```

- 위의 에제 코드처럼 리턴 타입이 `ResponseEntity<ErrorResponse>` 으로 무슨 데이터가 어떻게 있는지 명확하게 추론하기 쉽도록 구성하는게 바람직하다. 

## Error Response JSON

```json
{
  "message": " Invalid Input Value",
  "status": 400,
  // "errors":[], 비어있을 경우 null 이 아닌 빈 배열을 응답한다.
  "errors": [
    {
      "field": "name.last",
      "value": "",
      "reason": "must not be empty"
    },
    {
      "field": "name.first",
      "value": "",
      "reason": "must not be empty"
    }
  ],
  "code": "C001"
}
```

- message: 에러에 대한 message
- status: http status code, header 정보에도 포함된 정보이므로 굳이 추가하지 않아도 된다.
- errors: 요청 값에 대한 `field`, `value`, `reason` 작성, 일반적으로 `@Valid` 어노테이션으로 `JSR 303: Bean Validation` 에 대한 검증을 진행
  - 만약 errors에 바인딩된 결과가 없을 경우 null이 아니라 빈 배열 ([])을 응답
  - null 객체는 절대 리턴하지 않는다. null이 의미하는 것이 애매하기 때문
- code: 에러에 할당되는 유니크한 코드값

## Error Response 객체
```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse {

  private String message;
  private int status;
  private List<FieldError> errors;
  private String code;
    ...

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class FieldError {
    private String field;
    private String value;
    private String reason;
        ...
  }
}
```

- POJO 객체로 관리하면 `errorResponse.getXXX();` 이렇게 명확하게 객체에 있는 값을 가져올 수 있다.
> POJO?
> 
> POJO는 Java 언어 규약에 의해 강제된 것 이외의 제한에 구속되지 않는 Java 오브젝트


## @ControllerAdvice로 모든 예외를 핸들링

- `@ControllerAdvice` 어노테이션으로 모든 예외를 한 곳에서 처리 할 수 있다.

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     *  javax.validation.Valid or @Validated 으로 binding error 발생시 발생한다.
     *  HttpMessageConverter 에서 등록한 HttpMessageConverter binding 못할경우 발생
     *  주로 @RequestBody, @RequestPart 어노테이션에서 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("handleMethodArgumentNotValidException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * @ModelAttribut 으로 binding error 발생시 BindException 발생한다.
     * ref https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-modelattrib-method-args
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.error("handleBindException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * enum type 일치하지 않아 binding 못할 경우 발생
     * 주로 @RequestParam enum으로 binding 못했을 경우 발생
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("handleMethodArgumentTypeMismatchException", e);
        final ErrorResponse response = ErrorResponse.of(e);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 지원하지 않은 HTTP method 호출 할 경우 발생
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("handleHttpRequestMethodNotSupportedException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Authentication 객체가 필요한 권한을 보유하지 않은 경우 발생합
     */
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("handleAccessDeniedException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.HANDLE_ACCESS_DENIED);
        return new ResponseEntity<>(response, HttpStatus.valueOf(ErrorCode.HANDLE_ACCESS_DENIED.getStatus()));
    }

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(final BusinessException e) {
        log.error("handleEntityNotFoundException", e);
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse response = ErrorResponse.of(errorCode);
        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }


    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("handleEntityNotFoundException", e);
        final ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

- handleMethodArgumentNotValidException
  - javax.validation.Valid or @Validated 으로 binding error 발생시 발생
  - HttpMessageConverter에서 등록한 HttpMessageConverter binding 못할 경우 발생, 주로 @RequestBody, @RequestPart 어노테이션에서 발생
- handleBindException
  - @ModelAttribute 으로 binding error 발생시 BindException 발생
- MethodArgumentTypeMismatchException
  - Enum type이 일치하지 않아 binding 못할 경우 발생
  - 주로 @RequestParam enum으로 binding 못했을 경우 발생
- handleHttpRequestMethodNotSupportedException 
  - 지원하지 않은 HTTP method 호출 할 경우 발생
- handleAccessDeniedException
  - Authentication 객체가 필요한 권한을 보유하지 않은 경우 발생합
  - Security에서 던지는 예외
- handleException
  - 그 밖에 발생하는 모든 예외 처리, Null Point Exception, 등등
  - 개발자가 직접 핸들링해서 다른 예외로 던지지 않으면 모두 이곳으로 모인다.
- handleBusinessException
  - 비지니스 요구사항에 따른 Exception

추가로 스프링 및 라이브러리 등 자체적으로 발생하는 예외는 `@ExceptionHandler` 으로 추가해서 적절한 Error Response를 만들고 **비즈니스 요구사항에 예외일 경우 `BusinessException`으로 통일성 있게 처리하는것을 목표로 한다.**


## ErrorCode 정의

```java
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(400, "C001", " Invalid Input Value"),
    METHOD_NOT_ALLOWED(405, "C002", " Invalid Input Value"),
    ....
    HANDLE_ACCESS_DENIED(403, "C006", "Access is Denied"),

    // Member
    EMAIL_DUPLICATION(400, "M001", "Email is Duplication"),
    LOGIN_INPUT_INVALID(400, "M002", "Login input is invalid"),

    ;
    private final String code;
    private final String message;
    private int status;

    ErrorCode(final int status, final String code, final String message) {
        this.status = status;
        this.message = message;
        this.code = code;
    }
}
```

- 에러 코드는 enum 타입으로 한 곳에 관리
- 에러 코드가 전체적으로 흩어져있을 경우 코드, 메시지의 중복을 방지하기 어렵고 전체적으로 관리하는 것이 매우 어렵다.

## Business Exception 처리

- 여기서 말하는 Business Exception은 요구사항에 맞지 않을 경우 발생시키는 Exception
- 예를 들어, 쿠폰을 사용 하려고 하는데 이미 사용한 쿠폰인 경우에는 더 이상 정상적인 흐름을 진행할 수 없다.
- 이런 경우 적절한 Exception을 발생시키고 로직을 종료 해야함

## 비즈니스 예외를 위한 최상위 BusinessException 클래스 

- 최상위 BusinessException을 상속 받는 InvalidValueException, EntityNotFoundException 
  - InvalidValueException : 유효하지 않은 값일 경우 예외를 던지는 Excetion
    - 쿠폰 만료, 이미 사용한 쿠폰 등의 이유로 더이상 진행이 못할경우
  - EntityNotFoundException : 각 엔티티들을 못찾았을 경
    - findById, findByCode 메서드에서 조회가 안되었을 경우

최상위 BusinessException을 기준으로 예외를 발생시키면 통일감 있는 예외 처리를 가질 수 있습니다. 비니지스 로직을 수행하는 코드 흐름에서 로직의 흐름을 진행할 수 없는 상태인 경우에는 적절한 BusinessException 중에 하나를 예외를 발생 시키거나 직접 정의

```java
@ExceptionHandler(BusinessException.class)
protected ResponseEntity<ErrorResponse> handleBusinessException(final BusinessException e) {
    log.error("handleEntityNotFoundException", e);
    final ErrorCode errorCode = e.getErrorCode();
    final ErrorResponse response = ErrorResponse.of(errorCode);
    return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
}
```

- 이렇게 발생하는 모든 예외는 handleBusinessException 에서 동일하게 핸들링 됩니다. 예외 발생시 알람을 받는 등의 추가적인 행위도 손쉽게 가능
- 또 BusinessException 클래스의 하위 클래스 중에서 특정 예외에 대해서 다른 알람을 받는 등의 더 디테일한 핸들링도 가능

## 컨트롤러 예외 처리

- 컨트롤러에서 모든 요청에 대한 값 검증을 진행하고 이상이 없을 시에 서비스 레이어를 호출
- 잘못된 값이 있으면 서비스 레이어에서 정상적인 작업을 진행하기 어렵다.
- **무엇보다 컨트롤러의 책임을 다하고 있지 않으면 그 책임은 자연스럽게 다른 레이어로 전해지게 되며 이렇게 넘겨받은 책임을 처리하는데 큰 비용과 유지보수 하기 여려워질 수 밖에 없다.**
```java
@RestController
@RequestMapping("/members")
public class MemberApi {

    private final MemberSignUpService memberSignUpService;

    @PostMapping
    public MemberResponse create(@RequestBody @Valid final SignUpRequest dto) {
        final Member member = memberSignUpService.doSignUp(dto);
        return new MemberResponse(member);
    }
}
```

- 회원가입 Request Body 중에서 유효하지 않은 값이 있을 때 `@Valid` 어노테이션으로 예외를 발생시킬 수 있다.
- 이 예외는 `@ControllerAdvice` 에서 적절하게 핸들링 된다.

--- 
## `스프링 데이터`에서 제공하는 `QueryDsl` 기능

### Repository 인터페이스 지원: QuerydslPredicateExecutor

- `스프링 데이터`는 `QuerydslPredicateExecutor`라는 인터페이스를 제공

```java
import java.util.Optional;
import java.util.function.Predicate;

public interface QuerydslPredicateExecutor<T> {

    Optional<T> findById(Predicate); // (1)

    Iterable<T> findAll(Predicate predicate); // (2)

    long count(Predicate predicate); // (3)

    boolean exists(Predicate predicate); // (4)

}

```

> (1) Predicate에 매칭되는 하나의 Entity를 반환
>
> (2) Predicate에 매칭되는 모든 Entity를 반환합니다.
>
> (3) Predicate에 매칭되는 Entity의 수를 반환합니다.
>
> (4) Predicate에 매칭되는 결과가 있는지 여부를 반환합니다.

