/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.spi.security;

import java.lang.reflect.Field;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CompositeConfigurationTest extends AbstractCompositeConfigurationTest {

    private static final String NAME = "test";

    @Before
    public void before() throws Exception {
        compositeConfiguration = new CompositeConfiguration("test", new SecurityProvider() {
            @Nonnull
            @Override
            public ConfigurationParameters getParameters(@Nullable String name) {
                throw new UnsupportedOperationException();
            }

            @Nonnull
            @Override
            public Iterable<? extends SecurityConfiguration> getConfigurations() {
                throw new UnsupportedOperationException();
            }

            @Nonnull
            @Override
            public <T> T getConfiguration(@Nonnull Class<T> configClass) {
                throw new UnsupportedOperationException();
            }
        }) {};
    }

    @Test
    public void testGetName() {
        assertEquals(NAME, compositeConfiguration.getName());
    }

    @Test
    public void testEmpty() {
        assertSame(ConfigurationParameters.EMPTY, compositeConfiguration.getParameters());
        assertTrue(getConfigurations().isEmpty());
    }

    @Test
    public void testSetDefaultConfig() {
        SecurityConfiguration sc = new SecurityConfiguration.Default();
        setDefault(sc);

        List<SecurityConfiguration> configurations = getConfigurations();
        assertFalse(configurations.isEmpty());
        assertEquals(1, configurations.size());
        assertEquals(sc, configurations.iterator().next());
    }

    @Test
    public void testAddConfiguration() {
        addConfiguration(new SecurityConfiguration.Default());
        addConfiguration(new SecurityConfiguration.Default());

        List<SecurityConfiguration> configurations = getConfigurations();
        assertFalse(configurations.isEmpty());
        assertEquals(2, configurations.size());

        SecurityConfiguration def = new SecurityConfiguration.Default();
        setDefault(def);

        configurations = getConfigurations();
        assertEquals(2, configurations.size());
        assertFalse(configurations.contains(def));
    }

    @Test
    public void testRemoveConfiguration() {
        SecurityConfiguration def = new SecurityConfiguration.Default();
        setDefault(def);

        SecurityConfiguration sc = new SecurityConfiguration.Default();
        addConfiguration(sc);

        removeConfiguration(def);
        List configurations = getConfigurations();
        assertEquals(1, configurations.size());
        assertEquals(sc, configurations.iterator().next());

        removeConfiguration(sc);
        configurations = getConfigurations();
        assertEquals(1, configurations.size());
        assertEquals(def, configurations.iterator().next());
    }

    @Test
    public void testGetContext() throws Exception {
        Class cls = Class.forName(CompositeConfiguration.class.getName() + "$CompositeContext");
        Field def = cls.getDeclaredField("defaultCtx");
        def.setAccessible(true);

        Field delegatees = cls.getDeclaredField("delegatees");
        delegatees.setAccessible(true);

        Context ctx = compositeConfiguration.getContext();
        assertSame(cls, ctx.getClass());
        assertNull(delegatees.get(ctx));
        assertSame(Context.DEFAULT, def.get(ctx));

        SecurityConfiguration sc = new TestConfiguration();
        setDefault(sc);
        ctx = compositeConfiguration.getContext();
        assertNull(delegatees.get(ctx));
        assertSame(sc.getContext(), def.get(ctx));
        assertSame(cls, ctx.getClass());

        addConfiguration(sc);
        ctx = compositeConfiguration.getContext();
        assertNotSame(sc.getContext(), ctx);
        assertEquals(1, ((Context[]) delegatees.get(ctx)).length);

        // add configuration that has DEFAULT ctx -> must not be added
        SecurityConfiguration defConfig = new SecurityConfiguration.Default();
        addConfiguration(defConfig);
        assertEquals(1, ((Context[]) delegatees.get(compositeConfiguration.getContext())).length);

        // add same test configuration again -> no duplicate entries
        addConfiguration(sc);
        assertEquals(1, ((Context[]) delegatees.get(compositeConfiguration.getContext())).length);

        SecurityConfiguration sc2 = new TestConfiguration();
        addConfiguration(sc2);
        assertEquals(2, ((Context[]) delegatees.get(compositeConfiguration.getContext())).length);

        removeConfiguration(sc2);
        assertEquals(1, ((Context[]) delegatees.get(compositeConfiguration.getContext())).length);

        removeConfiguration(sc);
        removeConfiguration(sc);
        removeConfiguration(defConfig);
        assertNull(delegatees.get(compositeConfiguration.getContext()));
    }

    private static final class TestConfiguration extends SecurityConfiguration.Default {

        private final Context ctx = new TestContext();
        @Nonnull
        @Override
        public Context getContext() {
            return ctx;
        }
    }

    private static final class TestContext extends Context.Default {

    }
}