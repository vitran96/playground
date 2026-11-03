package demo.keycloak;

import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Registration form step: rejects company (@internal.email.com) emails and grants the "partner" role.
 * Used by: keycloak-2/3/4 "registration external" flow.
 */
public class ExternalRegistration implements FormAction, FormActionFactory {

    @Override
    public void validate(ValidationContext ctx) {
        MultivaluedMap<String, String> form = ctx.getHttpRequest().getDecodedFormParameters();
        String email = form.getFirst("email");
        if (email != null && email.trim().toLowerCase().endsWith(EmailOtpReset.INTERNAL_DOMAIN)) {
            ctx.error(Errors.INVALID_REGISTRATION);
            ctx.validationError(form, List.of(new FormMessage("email", "internalEmailNotAllowed")));
            return;
        }
        ctx.success();
    }

    @Override
    public void success(FormContext ctx) {
        RoleModel partner = ctx.getRealm().getRole("partner");
        if (partner != null) ctx.getUser().grantRole(partner);
    }

    @Override public void buildPage(FormContext ctx, LoginFormsProvider form) { }
    @Override public boolean requiresUser() { return false; }
    @Override public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) { return true; }
    @Override public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) { }
    @Override public void close() { }

    @Override public String getId() { return "external-registration"; }
    @Override public String getDisplayType() { return "External Registration (deny company domain, grant partner)"; }
    @Override public String getHelpText() { return "Rejects @internal.email.com sign-ups and grants the partner realm role."; }
    @Override public String getReferenceCategory() { return null; }
    @Override public boolean isConfigurable() { return false; }
    @Override public boolean isUserSetupAllowed() { return false; }
    @Override public Requirement[] getRequirementChoices() { return new Requirement[] { Requirement.REQUIRED, Requirement.DISABLED }; }
    @Override public List<ProviderConfigProperty> getConfigProperties() { return List.of(); }
    @Override public FormAction create(KeycloakSession session) { return this; }
    @Override public void init(Config.Scope config) { }
    @Override public void postInit(KeycloakSessionFactory factory) { }
}
