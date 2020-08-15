package org.cahsmun.registration.payment;

import com.google.gson.JsonSyntaxException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.cahsmun.registration.delegate.DelegateRepository;
import org.springframework.web.bind.annotation.*;
import spark.Request;
import spark.Response;

import javax.annotation.Resource;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@CrossOrigin()
@RestController
public class Listener {

    @Resource
    DelegateRepository delegateRepository; // will be used to save payment information if payment successful

    // @RequestMapping(value = "/listener", method = RequestMethod.POST)
    @RequestMapping(
            value = "/listener",
            method = RequestMethod.POST,
            consumes = "application/json")
    public String listener(@RequestBody HttpServletRequest request, HttpServletResponse response) throws Exception {
        return handler(request, response);
    }

    String httpServletRequestToString(HttpServletRequest request) throws Exception {

        ServletInputStream mServletInputStream = request.getInputStream();
        byte[] httpInData = new byte[request.getContentLength()];
        int retVal = -1;
        StringBuilder stringBuilder = new StringBuilder();

        while ((retVal = mServletInputStream.read(httpInData)) != -1) {
            for (int i = 0; i < retVal; i++) {
                stringBuilder.append(Character.toString((char) httpInData[i]));
            }
        }

        return stringBuilder.toString();
    }

    // Using the Spark framework (http://sparkjava.com)
    public String handler(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // Set your secret key. Remember to switch to your live secret key in production!
        // See your keys here: https://dashboard.stripe.com/account/apikeys
        Stripe.apiKey = "pk_test_Z4lkXkyCLtsOVpfnVi95obk60051Kx9aKg";

        // You can find your endpoint's secret in your webhook settings
        String endpointSecret = "whsec_MffGhsa9kXv0s7sbxeKHQ6F54qlHZrpA";

        String payload = httpServletRequestToString(request); // TODO: FIX the issue here
        String sigHeader = request.getHeader("Stripe-Signature");
        Event event = null;

        try {
            /*
            event = Event.GSON.fromJson(payload, Event.class);
            */
            event = Webhook.constructEvent(
                    payload, sigHeader, endpointSecret
            );

        } catch (JsonSyntaxException e) {
            // Invalid payload
            response.setStatus(400);
            return "";
        }  catch (SignatureVerificationException e) {
            // Invalid signature
            response.setStatus(400);
            return "";
        }/* catch (JsonSyntaxException e) {
            // Invalid payload
            response.status(400);
            return "";
        } */


        /*
        // Handle the checkout.session.completed event
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().get();

            // Fulfill the purchase...
            handleCheckoutSession(session);
        }
*/

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            // Deserialization failed, probably due to an API version mismatch.
            // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
            // instructions on how to handle this case, or return an error here.
            response.setStatus(400);
        }

        switch (event.getType()) {
            case "payment_intent.succeeded":
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                System.out.println("PaymentIntent was successful!");
                break;
            case "payment_method.attached":
                PaymentMethod paymentMethod = (PaymentMethod) stripeObject;
                System.out.println("PaymentMethod was attached to a Customer!");
                break;
            // ... handle other event types
            default:
                // Unexpected event type
                response.setStatus(400);
                return "";
        }


        response.setStatus(200);
        return "";
    }

    public void handleCheckoutSession(Session session) {

        // Check payment success/failure,
        // save payment data information into the delegate

    }
}
