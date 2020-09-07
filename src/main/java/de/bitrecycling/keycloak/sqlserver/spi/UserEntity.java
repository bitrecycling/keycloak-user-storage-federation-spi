package de.bitrecycling.keycloak.sqlserver.spi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * User model to represent user as stored in federated T_User from pathowin db
 */
@Data
@AllArgsConstructor
@NoArgsConstructor

@NamedQueries({
        @NamedQuery(name="getUserByUsername", query="select u from UserEntity u where u.login = :login"),
//        @NamedQuery(name="getUserByEmail", query="select u from UserEntity u where u.email = :email"),
        @NamedQuery(name="getUserCount", query="select count(u) from UserEntity u"),
        @NamedQuery(name="getAllUsers", query="select u from UserEntity u"),
//        @NamedQuery(name="searchForUser", query="select u from UserEntity u where " +
//                "( lower(u.login) like :search or u.email like :search ) order by u.login"),
})
@Entity
@Table(name = "T_User",schema="dbo")
public class UserEntity {
  
  
  
  @Id
  private String id;
//  private String email;
  @Column(name = "Login")
  private String login;
  private String bezeichnung;
  private String passwort;
  private String mitarbeiter_ID;
  private String dictaphonerolle_ID;
  private String nTLogin;
  private String xml;
  private String aktiv;
  private String klinik_ID;
  private String autoLogin;
  private String passwortGueltigBis;
  private String alleMandanten;
 
}
