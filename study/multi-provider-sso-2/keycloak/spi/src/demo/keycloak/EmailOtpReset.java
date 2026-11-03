package demo.keycloak;

import jakarta.ws.rs.core.MultivaluedMap;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Reset-credentials step: emails a 6-digit one-time code (instead of a link) and verifies it.
 * Used by: keycloak-2/3/4 "reset credentials email otp" flow (external users only).
 */
public class EmailOtpReset implements Authenticator, AuthenticatorFactory {

    static final String INTERNAL_DOMAIN = "@internal.email.com";
    private static final Logger LOG = Logger.getLogger(EmailOtpReset.class);
    private static final SecureRandom RNG = new SecureRandom();
    private static final String CODE = "email-otp-code", EXP = "email-otp-exp", TRIES = "email-otp-tries", SENDS = "email-otp-sends";
    private static final int TTL_SECONDS = 600, MAX_TRIES = 5, MAX_SENDS = 3;
    private static final String FORM = "login-email-otp.ftl";

    @Override
    public void authenticate(AuthenticationFlowContext ctx) {
        AuthenticationSessionModel s = ctx.getAuthenticationSession();
        s.removeAuthNote(CODE);
        int sends = s.getAuthNote(SENDS) == null ? 0 : Integer.parseInt(s.getAuthNote(SENDS));
        if (sends >= MAX_SENDS) {
            fail(ctx, "emailOtpTooMany");
            return;
        }
        s.setAuthNote(SENDS, Integer.toString(sends + 1));
        UserModel user = ctx.getUser();
        // Unknown, disabled, internal or brokered users get the same screen but no code (no enumeration,
        // and no local password can be planted on an internal identity).
        if (isExternalLocalUser(ctx.getSession(), ctx.getRealm(), user)) {
            String code = String.format("%06d", RNG.nextInt(1_000_000));
            try {
                ctx.getSession().getProvider(EmailSenderProvider.class).send(
                        ctx.getRealm().getSmtpConfig(), user,
                        "Your password reset code",
                        "Your password reset code is: " + code + "\nIt expires in 10 minutes.",
                        "<p>Your password reset code is: <b>" + code + "</b></p><p>It expires in 10 minutes.</p>");
            } catch (EmailException e) {
                LOG.error("Failed to send reset code email", e);
                ctx.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                        ctx.form().setError("emailSendErrorMessage").createErrorPage(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR));
                return;
            }
            s.setAuthNote(CODE, code);
            s.setAuthNote(EXP, Long.toString(System.currentTimeMillis() / 1000 + TTL_SECONDS));
            s.setAuthNote(TRIES, "0");
        }
        ctx.challenge(ctx.form().setInfo("emailOtpSent").createForm(FORM));
    }

    @Override
    public void action(AuthenticationFlowContext ctx) {
        MultivaluedMap<String, String> form = ctx.getHttpRequest().getDecodedFormParameters();
        if (form.containsKey("resend")) {
            authenticate(ctx);
            return;
        }
        AuthenticationSessionModel s = ctx.getAuthenticationSession();
        String expected = s.getAuthNote(CODE);
        String entered = form.getFirst("emailCode") == null ? "" : form.getFirst("emailCode").trim();

        if (expected == null) {
            fail(ctx, "emailOtpInvalid");
            return;
        }
        int tries = Integer.parseInt(s.getAuthNote(TRIES)) + 1;
        s.setAuthNote(TRIES, Integer.toString(tries));
        if (tries > MAX_TRIES || System.currentTimeMillis() / 1000 > Long.parseLong(s.getAuthNote(EXP))) {
            s.removeAuthNote(CODE);
            fail(ctx, "emailOtpExpired");
            return;
        }
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), entered.getBytes(StandardCharsets.UTF_8))) {
            fail(ctx, "emailOtpInvalid");
            return;
        }
        s.removeAuthNote(CODE);
        ctx.getUser().setEmailVerified(true);
        ctx.success();
    }

    private void fail(AuthenticationFlowContext ctx, String msg) {
        ctx.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                ctx.form().addError(new FormMessage("emailCode", msg)).createForm(FORM));
    }

    static boolean isExternalLocalUser(KeycloakSession session, RealmModel realm, UserModel user) {
        return user != null && user.isEnabled() && user.getEmail() != null
                && !user.getEmail().toLowerCase().endsWith(INTERNAL_DOMAIN)
                && session.users().getFederatedIdentitiesStream(realm, user).findAny().isEmpty();
    }

    @Override public boolean requiresUser() { return false; }
    @Override public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) { return true; }
    @Override public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) { }
    @Override public void close() { }

    @Override public String getId() { return "email-otp-reset"; }
    @Override public String getDisplayType() { return "Send Reset Code (Email OTP)"; }
    @Override public String getHelpText() { return "Emails a 6-digit one-time code and verifies it. External, non-brokered users only."; }
    @Override public String getReferenceCategory() { return null; }
    @Override public boolean isConfigurable() { return false; }
    @Override public boolean isUserSetupAllowed() { return false; }
    @Override public Requirement[] getRequirementChoices() { return new Requirement[] { Requirement.REQUIRED }; }
    @Override public List<ProviderConfigProperty> getConfigProperties() { return List.of(); }
    @Override public Authenticator create(KeycloakSession session) { return this; }
    @Override public void init(Config.Scope config) { }
    @Override public void postInit(KeycloakSessionFactory factory) { }
}
