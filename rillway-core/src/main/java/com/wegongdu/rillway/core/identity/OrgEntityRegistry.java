package com.wegongdu.rillway.core.identity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Declarative registry allowing developers to simply register their existing JavaBean / Entity classes
 * (e.g. UserDO.class, DeptDO.class, PostDO.class, RoleDO.class) without writing any custom SQL or IdentityService implementations.
 */
public record OrgEntityRegistry(
        Class<?> userEntityClass,
        Class<?> deptEntityClass,
        Class<?> postEntityClass,
        Class<?> roleEntityClass
) implements Serializable {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Class<?> userEntityClass;
        private Class<?> deptEntityClass;
        private Class<?> postEntityClass;
        private Class<?> roleEntityClass;

        public Builder userEntity(Class<?> userEntityClass) {
            this.userEntityClass = userEntityClass;
            return this;
        }

        public Builder deptEntity(Class<?> deptEntityClass) {
            this.deptEntityClass = deptEntityClass;
            return this;
        }

        public Builder postEntity(Class<?> postEntityClass) {
            this.postEntityClass = postEntityClass;
            return this;
        }

        public Builder roleEntity(Class<?> roleEntityClass) {
            this.roleEntityClass = roleEntityClass;
            return this;
        }

        public OrgEntityRegistry build() {
            return new OrgEntityRegistry(userEntityClass, deptEntityClass, postEntityClass, roleEntityClass);
        }
    }
}
