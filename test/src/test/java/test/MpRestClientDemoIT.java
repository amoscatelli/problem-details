package test;

import com.github.t1.jaxrslog.LoggingFilter;
import com.github.t1.problemdetail.ri.ProblemDetailResponseExceptionMapper;
import com.github.t1.problemdetaildemoapp.DemoService.CreditCardLimitExceeded;
import com.github.t1.problemdetaildemoapp.DemoService.UserNotEntitledToOrderOnAccount;
import com.github.t1.problemdetaildemoapp.OutOfCreditException;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static test.ContainerLaunchingExtension.baseUri;

/**
 * Demonstrate the MP RestClient side when mapping exceptions to problem details
 * as presented in the rfc.
 */
@ExtendWith(ContainerLaunchingExtension.class)
public class MpRestClientDemoIT extends AbstractClientDemoIT {
    @Path("/orders")
    public interface OrderApi {
        @POST Shipment order(
            @FormParam("user") int userId,
            @FormParam("article") @NotNull String article,
            @FormParam("payment-method") String paymentMethod)
            throws OutOfCreditException, CreditCardLimitExceeded, UserNotEntitledToOrderOnAccount;
    }

    private OrderApi api = RestClientBuilder.newBuilder()
        .baseUri(baseUri(""))
        .register(ProblemDetailResponseExceptionMapper.class) // not required in container
        .register(LoggingFilter.toStdErr())
        .build(OrderApi.class);

    @Override protected Shipment postOrder(String userId, String article, String paymentMethod) {
        return api.order(Integer.parseInt(userId), article, paymentMethod);
    }
}
