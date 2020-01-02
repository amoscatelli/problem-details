package test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.t1.problemdetail.ri.lib.ProblemDetailExceptionRegistry;
import com.github.t1.problemdetail.spring.ProblemDetailErrorHandler;
import com.github.t1.problemdetaildemoapp.DemoService.UserNotEntitledToOrderOnAccount;
import com.github.t1.problemdetaildemoapp.OutOfCreditException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static com.github.t1.problemdetaildemoapp.DemoService.ACCOUNT_1;
import static com.github.t1.problemdetaildemoapp.DemoService.ACCOUNT_2;
import static com.github.t1.problemdetaildemoapp.DemoService.PROBLEM_INSTANCE;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static test.ContainerLaunchingExtension.BASE_URI;

/**
 * Demonstrate the client side when mapping exceptions to problem details
 * as presented in the rfc.
 */
@ExtendWith(ContainerLaunchingExtension.class)
class SpringDemoIT {
    static {
        ProblemDetailExceptionRegistry.register(OutOfCreditException.class);
        ProblemDetailExceptionRegistry.register(UserNotEntitledToOrderOnAccount.class);
    }

    @Test void shouldOrderCheapGadget() {
        Shipment shipment = postOrder("1", "cheap gadget", null);

        then(shipment).isEqualTo(new Shipment(
            "1:cheap gadget:" + LocalDate.now(),
            "cheap gadget",
            1));
    }

    @Test void shouldFailToOrderGadgetWhenUserNotEntitledToOrderOnAccount() {
        UserNotEntitledToOrderOnAccount throwable = catchThrowableOfType(() -> postOrder("2", "cheap gadget", "on_account"),
            UserNotEntitledToOrderOnAccount.class);

        then(throwable).describedAs("nothing thrown").isNotNull();
    }

    @Test void shouldFailToOrderExpensiveGadgetWhenOutOfCredit() {
        OutOfCreditException throwable = catchThrowableOfType(() -> postOrder("1", "expensive gadget", null),
            OutOfCreditException.class);

        then(throwable).describedAs("nothing thrown").isNotNull();
        then(throwable.getBalance()).isEqualTo(30);
        then(throwable.getCost()).isEqualTo(0); // not an extension, i.e. not in the body
        then(throwable.getInstance()).isEqualTo(PROBLEM_INSTANCE);
        // detail is not settable, i.e. it's recreated in the method and the cost is 0
        then(throwable.getDetail()).isEqualTo("Your current balance is 30, but that costs 0.");
        then(throwable.getAccounts()).containsExactly(ACCOUNT_1, ACCOUNT_2);
    }

    private Shipment postOrder(String userId, String article, String paymentMethod) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("user", userId);
        form.add("article", article);
        form.add("payment-method", paymentMethod);

        RestTemplate template = new RestTemplate();
        template.setErrorHandler(new ProblemDetailErrorHandler());
        return template.postForObject(BASE_URI + "/orders", form, Shipment.class);
    }

    @AllArgsConstructor @NoArgsConstructor
    public static @Data class Shipment {
        @JsonProperty("shipment-id") String shipmentId;
        String article;
        Integer user;
    }
}
