package de.bitrecycling.keycloak.sqlserver.spi;

import lombok.SneakyThrows;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.util.List;

import static org.keycloak.models.AbstractKeycloakTransaction.logger;

/**
 * provides functionality to lookup / load user from federated (sqlserver?) db
 */
@Local(SimpleUserStorageProvider.class)
@Stateful
@JBossLog
public class SimpleUserStorageProvider implements UserStorageProvider,
  UserLookupProvider, CredentialInputValidator {

  public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";

  @PersistenceContext
  protected EntityManager em;
  protected ComponentModel model;
  protected KeycloakSession session;
  

  @Override
  public UserModel getUserById(String id, RealmModel realm) {
    logger.debug("getUserById: " + id);
    String persistenceId = StorageId.externalId(id);
    UserEntity entity = em.find(UserEntity.class, persistenceId);
    if (entity == null) {
      logger.info("could not find user by id: " + id);
      return null;
    }
    return new UserAdapter(session, realm, model, entity);
  }

  @Override
  public UserModel getUserByUsername(String username, RealmModel realm) {
    logger.debug("getUserByUsername: " + username);
    TypedQuery<UserEntity> query = em.createNamedQuery("getUserByUsername", UserEntity.class);
    query.setParameter("login", username);
    List<UserEntity> result = query.getResultList();
    if (result.isEmpty()) {
      logger.info("could not find login: " + username);
      return null;
    }
    
    else {
      logger.info("FOUND:"+result.toString());
    }

    return new UserAdapter(session, realm, model, result.get(0));
  }

  @Override
  public UserModel getUserByEmail(String email, RealmModel realm) {
    throw new RuntimeException("not implemented!");
  }
  
  @Override
  public boolean supportsCredentialType(String credentialType) {
    logger.warn("supports credential type? :"+credentialType+" ==> " +PasswordCredentialModel.TYPE.equals(credentialType));
    return PasswordCredentialModel.TYPE.equals(credentialType);
  }
  
  @Override
  public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
    return supportsCredentialType(credentialType) && getPassword(user) != null;
  }

  @Override
  @SneakyThrows
  public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
    if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)){
      return false;
    } 
    UserCredentialModel given = (UserCredentialModel)input;
    logger.warn("given password="+given.getChallengeResponse());
    MessageDigest md = MessageDigest.getInstance("MD5");
    md.update(given.getChallengeResponse().getBytes());
    final byte[] digest = md.digest();
    String givenPWmd5 = DatatypeConverter
            .printHexBinary(digest).toUpperCase();
    logger.warn("md5 of given="+givenPWmd5);
  
    String storedPassword = getPassword(user);
    logger.warn("stored password="+storedPassword);
    return storedPassword != null && storedPassword.equals(givenPWmd5);
  }

  public String getPassword(UserModel user) {
    
    String password = null;
    if (user instanceof CachedUserModel) {
      password = (String)((CachedUserModel)user).getCachedWith().get(PASSWORD_CACHE_KEY);
    } else if (user instanceof UserAdapter) {
      password = ((UserAdapter)user).getPassword();
    }
    return password;
    
  }

  public void setModel(ComponentModel model) {
    this.model = model;
  }

  public void setSession(KeycloakSession session) {
    this.session = session;
  }

  @Override
  public void preRemove(RealmModel realm) {
    //nothing to be done before removal
  }

  

  @Override
  public void preRemove(RealmModel realm, RoleModel role) {
    //nothing to be done before removal
  }

  @Override
  public void close() {
    //nothing to be done on close
  }
  
}
