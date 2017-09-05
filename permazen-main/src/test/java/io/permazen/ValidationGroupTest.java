
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package io.permazen;

import io.permazen.annotation.JField;
import io.permazen.annotation.JSimpleClass;
import io.permazen.annotation.OnValidate;
import io.permazen.test.TestSupport;

import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.testng.annotations.Test;

public class ValidationGroupTest extends TestSupport {

    @Test
    public void testValidationGroups() {
        final JSimpleDB jdb = BasicTest.getJSimpleDB(Foobar.class);
        JTransaction jtx = jdb.createTransaction(true, ValidationMode.MANUAL);
        JTransaction.setCurrent(jtx);
        try {

            // Create instances
            final Foobar f1 = jtx.create(Foobar.class);

        // Field A

            f1.reset();
            f1.setFieldA(null);
            this.check(f1, false);

            f1.reset();
            f1.setFieldA(null);
            this.check(f1, false, Default.class);

            f1.reset();
            f1.setFieldA(null);
            this.check(f1, false, Group1.class);

            f1.reset();
            f1.setFieldA(null);
            this.check(f1, true, Group2.class);

        // Field B

            f1.reset();
            f1.setFieldB(null);
            this.check(f1, true);

            f1.reset();
            f1.setFieldB(null);
            this.check(f1, true, Default.class);

            f1.reset();
            f1.setFieldB(null);
            this.check(f1, false, Group1.class);

            f1.reset();
            f1.setFieldB(null);
            this.check(f1, true, Group2.class);

        // Field C

            f1.reset();
            f1.setFieldC(null);
            this.check(f1, true);

            f1.reset();
            f1.setFieldC(null);
            this.check(f1, true, Default.class);

            f1.reset();
            f1.setFieldC(null);
            this.check(f1, true, Group1.class);

            f1.reset();
            f1.setFieldC(null);
            this.check(f1, false, Group2.class);

        // Field D

            f1.reset();
            f1.setFieldD(null);
            this.check(f1, false);

            f1.reset();
            f1.setFieldD(null);
            this.check(f1, false, Default.class);

            f1.reset();
            f1.setFieldD(null);
            this.check(f1, false, Group1.class);

            f1.reset();
            f1.setFieldD(null);
            this.check(f1, false, Group2.class);

        // Field E

            final Foobar f2 = jtx.create(Foobar.class);

            f1.reset();
            f2.reset();
            f1.setFieldE("abc");
            f2.setFieldE("abc");
            this.check(f1, false);
            this.check(f1, false, Default.class);
            this.check(f1, false, UniquenessConstraints.class);
            this.check(f1, false, Default.class, UniquenessConstraints.class);

            f1.setFieldE("def");
            this.check(f1, true);
            this.check(f1, true, Default.class);
            this.check(f1, true, UniquenessConstraints.class);
            this.check(f1, true, Default.class, UniquenessConstraints.class);

            f1.setFieldA(null);
            this.check(f1, false);
            this.check(f1, true, UniquenessConstraints.class);
            f1.reset();

            f1.setFieldE(null);
            f2.setFieldE(null);
            this.check(f1, true, UniquenessConstraints.class);

            jtx.commit();
        } finally {
            JTransaction.setCurrent(null);
        }
    }

    private void check(Foobar f, boolean valid, Class<?>... groups) {
        f.revalidate(groups);
        try {
            f.getTransaction().validate();
            assert valid;
        } catch (ValidationException e) {
            assert !valid;
        }
    }

// Model Classes

    public interface Group1 extends Default { }

    public interface Group2 { }

    @JSimpleClass
    public abstract static class Foobar implements JObject {

        private String enableChecks;

        public void setEnableChecks(String enableChecks) {
            this.enableChecks = enableChecks;
        }

        public void reset() {
            this.setFieldA("a");
            this.setFieldB("b");
            this.setFieldC("c");
            this.setFieldD("d");
            this.setFieldE("" + this.getObjId());
        }

        @NotNull
        public abstract String getFieldA();
        public abstract void setFieldA(String fieldA);

        @NotNull(groups = Group1.class)
        public abstract String getFieldB();
        public abstract void setFieldB(String fieldB);

        @NotNull(groups = Group2.class)
        public abstract String getFieldC();
        public abstract void setFieldC(String fieldC);

        @NotNull(groups = { Default.class, Group2.class })
        public abstract String getFieldD();
        public abstract void setFieldD(String fieldD);

        @JField(indexed = true, unique = true, uniqueExclude = JField.NULL)
        public abstract String getFieldE();
        public abstract void setFieldE(String fieldE);

        @OnValidate
        private void checkA() {
            if (this.enableChecks != null && this.enableChecks.indexOf('A') != -1)
                throw new ValidationException(this, "checkA");
        }

        @OnValidate(groups = Group1.class)
        private void checkB() {
            if (this.enableChecks != null && this.enableChecks.indexOf('B') != -1)
                throw new ValidationException(this, "checkB");
        }

        @OnValidate(groups = Group2.class)
        private void checkC() {
            if (this.enableChecks != null && this.enableChecks.indexOf('C') != -1)
                throw new ValidationException(this, "checkC");
        }

        @OnValidate(groups = { Default.class, Group2.class })
        private void checkD() {
            if (this.enableChecks != null && this.enableChecks.indexOf('D') != -1)
                throw new ValidationException(this, "checkD");
        }
    }
}

