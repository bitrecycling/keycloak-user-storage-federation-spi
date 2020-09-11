package de.bitrecycling.keycloak.sqlserver.spi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * User model to represent user as stored in federated User from demo db
 */
@Data
@AllArgsConstructor
@NoArgsConstructor

@NamedQueries({
        @NamedQuery(name="getUserByUsername", query="select u from UserEntity u where u.username = :username"),
        @NamedQuery(name="getUserCount", query="select count(u) from UserEntity u"),
        @NamedQuery(name="getAllUsers", query="select u from UserEntity u"),
        @NamedQuery(name="getByEmail", query="select u from UserEntity u where u.email = :email"),

})
@Entity
@Table(name = "User",schema="dbo")
public class UserEntity {
    
  @Id
  private String id;
  private String username;
  private String password;
  private String email;
}
