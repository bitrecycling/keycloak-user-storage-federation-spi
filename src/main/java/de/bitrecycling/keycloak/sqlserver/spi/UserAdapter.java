package de.bitrecycling.keycloak.sqlserver.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

public class UserAdapter extends AbstractUserAdapterFederatedStorage {

  private final UserEntity userEntity;
  private final String keycloakId;

  public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserEntity userEntity) {
    super(session, realm, model);
    this.userEntity = userEntity;
    this.keycloakId = StorageId.keycloakId(model, userEntity.getId());
  }

  @Override
  public String getId() {
    return keycloakId;
  }

  @Override
  public String getUsername() {
    return userEntity.getLogin();
  }

  @Override
  public void setUsername(String username) {
    userEntity.setLogin(username);
  }

  @Override
  public String getEmail() {
    return userEntity.getBezeichnung();
  }

  @Override
  public void setEmail(String email) {
    userEntity.setBezeichnung(email);
  }

  @Override
  public String getFirstName() {
    return userEntity.getLogin();
  }

  @Override
  public void setFirstName(String firstName) {
    userEntity.setLogin(firstName);
  }

  @Override
  public String getLastName() {
    return userEntity.getLogin();
  }

  @Override
  public void setLastName(String lastName) {
    userEntity.setLogin(lastName);
  }
  
  public String getPassword(){
    return userEntity.getPasswort();
  }
}
