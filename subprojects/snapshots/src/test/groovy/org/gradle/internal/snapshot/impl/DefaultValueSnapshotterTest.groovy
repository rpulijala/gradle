/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.snapshot.impl

import org.gradle.api.Named
import org.gradle.api.internal.model.NamedObjectInstantiator
import org.gradle.api.provider.Provider
import org.gradle.internal.classloader.ClassLoaderHierarchyHasher
import org.gradle.internal.classloader.ClasspathUtil
import org.gradle.internal.classloader.FilteringClassLoader
import org.gradle.internal.hash.HashCode
import org.gradle.internal.snapshot.ValueSnapshot
import org.gradle.test.fixtures.file.TestFile
import spock.lang.Specification

class DefaultValueSnapshotterTest extends Specification {
    def classLoaderHasher = Stub(ClassLoaderHierarchyHasher) {
        getClassLoaderHash(_) >> HashCode.fromInt(123)
    }
    def snapshotter = new DefaultValueSnapshotter(classLoaderHasher, NamedObjectInstantiator.INSTANCE)

    def "creates snapshot for string"() {
        expect:
        def snapshot = snapshotter.snapshot("abc")
        snapshot instanceof StringValueSnapshot
        snapshot == snapshotter.snapshot("abc")
        snapshot != snapshotter.snapshot("other")
    }

    def "creates isolated string"() {
        expect:
        def isolated = snapshotter.isolate("abc")
        isolated instanceof StringValueSnapshot
        isolated.isolate() == "abc"
    }

    def "can coerce string value"() {
        expect:
        def isolated = snapshotter.isolate("abc")
        isolated.coerce(String).is(isolated)
        isolated.coerce(CharSequence).is(isolated)
        isolated.coerce(Object).is(isolated)
        isolated.coerce(Number) == null
    }

    def "creates snapshot for integer"() {
        expect:
        def snapshot = snapshotter.snapshot(123)
        snapshot instanceof IntegerValueSnapshot
        snapshot == snapshotter.snapshot(123)
        snapshot != snapshotter.snapshot(-1)
        snapshot != snapshotter.snapshot(123L)
        snapshot != snapshotter.snapshot(123 as short)
        snapshot != snapshotter.snapshot(123 as BigDecimal)
    }

    def "creates isolated integer"() {
        expect:
        def isolated = snapshotter.isolate(123)
        isolated instanceof IntegerValueSnapshot
        isolated.isolate() == 123
    }

    def "can coerce integer value"() {
        expect:
        def isolated = snapshotter.isolate(123)
        isolated.coerce(Integer).is(isolated)
        isolated.coerce(Number).is(isolated)
        isolated.coerce(Long) == null
        isolated.coerce(Short) == null
        isolated.coerce(Byte) == null
        isolated.coerce(String) == null
    }

    def "creates snapshot for long"() {
        expect:
        def snapshot = snapshotter.snapshot(123L)
        snapshot instanceof LongValueSnapshot
        snapshot == snapshotter.snapshot(123L)
        snapshot != snapshotter.snapshot(-1L)
        snapshot != snapshotter.snapshot(123)
        snapshot != snapshotter.snapshot(123 as short)
        snapshot != snapshotter.snapshot(123 as BigDecimal)
    }

    def "creates isolated long"() {
        expect:
        def isolated = snapshotter.isolate(123L)
        isolated instanceof LongValueSnapshot
        isolated.isolate() == 123L
    }

    def "can coerce long value"() {
        expect:
        def isolated = snapshotter.isolate(123L)
        isolated.coerce(Long).is(isolated)
        isolated.coerce(Number).is(isolated)
        isolated.coerce(Integer) == null
        isolated.coerce(Short) == null
        isolated.coerce(Byte) == null
        isolated.coerce(String) == null
    }

    def "creates snapshot for short"() {
        expect:
        def snapshot = snapshotter.snapshot(123 as short)
        snapshot instanceof ShortValueSnapshot
        snapshot == snapshotter.snapshot(123 as short)
        snapshot != snapshotter.snapshot(-1L)
        snapshot != snapshotter.snapshot(123)
        snapshot != snapshotter.snapshot(123L)
        snapshot != snapshotter.snapshot(123 as BigDecimal)
    }

    def "creates isolated short"() {
        expect:
        def isolated = snapshotter.isolate(123 as short)
        isolated instanceof ShortValueSnapshot
        isolated.isolate() == 123 as short
    }

    def "can coerce short value"() {
        expect:
        def isolated = snapshotter.isolate(123 as short)
        isolated.coerce(Short).is(isolated)
        isolated.coerce(Number).is(isolated)
        isolated.coerce(Integer) == null
        isolated.coerce(Byte) == null
        isolated.coerce(String) == null
    }

    def "creates snapshot for boolean"() {
        expect:
        snapshotter.snapshot(true).is BooleanValueSnapshot.TRUE
        snapshotter.snapshot(false).is BooleanValueSnapshot.FALSE
    }

    def "creates isolated boolean"() {
        expect:
        snapshotter.isolate(true).isolate()
        !snapshotter.isolate(false).isolate()
    }

    def "can coerce boolean value"() {
        expect:
        snapshotter.isolate(true).coerce(Boolean).is BooleanValueSnapshot.TRUE
        snapshotter.isolate(false).coerce(Boolean).is BooleanValueSnapshot.FALSE
        snapshotter.isolate(false).coerce(String) == null
    }

    def "creates snapshot for null value"() {
        expect:
        snapshotter.snapshot(null).is NullValueSnapshot.INSTANCE
    }

    def "creates isolated null value"() {
        expect:
        snapshotter.isolate(null).isolate() == null
    }

    def "can coerce null value"() {
        expect:
        snapshotter.isolate(null).coerce(String).is NullValueSnapshot.INSTANCE
    }

    def "creates snapshot for array"() {
        expect:
        def snapshot1 = snapshotter.snapshot([] as String[])
        snapshot1 instanceof ArrayValueSnapshot
        snapshot1 == snapshotter.snapshot([] as String[])
        snapshot1 == snapshotter.snapshot([] as Object[])
        snapshot1 == snapshotter.snapshot([] as Integer[])
        snapshot1 != snapshotter.snapshot("abc" as String[])
        snapshot1 != snapshotter.snapshot([])

        def snapshot2 = snapshotter.snapshot(["123"] as String[])
        snapshot2 instanceof ArrayValueSnapshot
        snapshot2 == snapshotter.snapshot(["123"] as String[])
        snapshot2 == snapshotter.snapshot(["123"] as CharSequence[])
        snapshot2 == snapshotter.snapshot(["123"] as Object[])
        snapshot2 != snapshotter.snapshot(["123"])
        snapshot2 != snapshotter.snapshot("123")
        snapshot2 != snapshot1
    }

    def "creates isolated array"() {
        expect:
        def isolated1 = snapshotter.isolate([] as String[])
        isolated1 instanceof ArrayValueSnapshot
        isolated1.isolate() == [] as String[]

        def isolated2 = snapshotter.isolate(["123"] as String[])
        isolated2 instanceof ArrayValueSnapshot
        isolated2.isolate() == ["123"] as String[]
    }

    def "creates snapshot for list"() {
        expect:
        def snapshot1 = snapshotter.snapshot([])
        snapshot1 instanceof ListValueSnapshot
        snapshot1 == snapshotter.snapshot([])
        snapshot1 != snapshotter.snapshot("abc")

        def snapshot2 = snapshotter.snapshot(["123"])
        snapshot2 instanceof ListValueSnapshot
        snapshot2 == snapshotter.snapshot(["123"])
        snapshot2 != snapshot1
    }

    def "creates isolated list"() {
        expect:
        def isolated1 = snapshotter.isolate([])
        isolated1 instanceof ListValueSnapshot
        isolated1.isolate() == []

        def isolated2 = snapshotter.isolate(["123"])
        isolated2 instanceof ListValueSnapshot
        isolated2.isolate() == ["123"]
    }

    def "creates snapshot for list from empty list"() {
        def snapshot1 = snapshotter.snapshot([])
        def snapshot2 = snapshotter.snapshot(["123"], snapshot1)

        expect:
        snapshot2 instanceof ListValueSnapshot
        snapshot2 == snapshotter.snapshot(["123"])
        snapshot2 != snapshot1
    }

    def "creates snapshot for set"() {
        expect:
        def snapshot1 = snapshotter.snapshot([] as Set)
        snapshot1 instanceof SetValueSnapshot
        snapshot1 == snapshotter.snapshot([] as Set)
        snapshot1 != snapshotter.snapshot("abc")
        snapshot1 != snapshotter.snapshot([])

        def snapshot2 = snapshotter.snapshot(["123"] as Set)
        snapshot2 instanceof SetValueSnapshot
        snapshot2 == snapshotter.snapshot(["123"] as Set)
        snapshot2 != snapshotter.snapshot(["123"])
        snapshot2 != snapshot1
    }

    def "creates isolated set"() {
        expect:
        def isolated1 = snapshotter.isolate([] as Set)
        isolated1 instanceof SetValueSnapshot
        isolated1.isolate() == [] as Set

        def isolated2 = snapshotter.isolate(["123"] as Set)
        isolated2 instanceof SetValueSnapshot
        isolated2.isolate() == ["123"] as Set
    }

    def "creates snapshot for map"() {
        expect:
        def snapshot1 = snapshotter.snapshot([:])
        snapshot1 instanceof MapValueSnapshot
        snapshot1 == snapshotter.snapshot([:])
        snapshot1 != snapshotter.snapshot("abc")
        snapshot1 != snapshotter.snapshot([a: "123"])

        def snapshot2 = snapshotter.snapshot([a: "123"])
        snapshot2 instanceof MapValueSnapshot
        snapshot2 == snapshotter.snapshot([a: "123"])
        snapshot2 != snapshotter.snapshot(["123"])
        snapshot2 != snapshotter.snapshot([:])
        snapshot2 != snapshotter.snapshot([a: "123", b: "abc"])
        snapshot2 != snapshot1
    }

    def "creates isolated map"() {
        expect:
        def isolated1 = snapshotter.isolate([:])
        isolated1 instanceof MapValueSnapshot
        isolated1.isolate() == [:]

        def isolated2 = snapshotter.isolate([a: "123"])
        isolated2 instanceof MapValueSnapshot
        isolated2.isolate() == [a: "123"]
    }

    enum Type2 {
        TWO, THREE
    }

    def "creates snapshot for enum type"() {
        expect:
        def snapshot = snapshotter.snapshot(Type1.TWO)
        snapshot instanceof EnumValueSnapshot
        snapshot == snapshotter.snapshot(Type1.TWO)
        snapshot != snapshotter.snapshot(Type1.ONE)
        snapshot != snapshotter.snapshot(Type2.TWO)
        snapshot != snapshotter.snapshot(new Bean(prop: "value2"))
    }

    def "creates isolated enum value"() {
        expect:
        def isolated = snapshotter.isolate(Type1.TWO)
        isolated instanceof EnumValueSnapshot
        isolated.isolate() == Type1.TWO
    }

    def "can coerce enum value"() {
        def loader = new GroovyClassLoader(getClass().getClassLoader().parent)
        loader.addURL(ClasspathUtil.getClasspathForClass(GroovyObject).toURI().toURL())
        def cl = loader.parseClass("package ${Type1.package.name}; enum Type1 { TWO, THREE } ")
        assert cl != Type1
        assert cl.name == Type1.name

        expect:
        def isolated = snapshotter.isolate(Type1.TWO)
        isolated.coerce(Type1).is(isolated)
        isolated.coerce(Type2) == null
        isolated.coerce(String) == null

        def v = isolated.coerce(cl).isolate()
        cl.isInstance(v)
        v.name() == "TWO"
    }

    def "creates snapshot for file"() {
        expect:
        def snapshot = snapshotter.snapshot(new File("abc"))
        snapshot instanceof FileValueSnapshot
        snapshot == snapshotter.snapshot(new File("abc"))
        snapshot != snapshotter.snapshot(new File("abc").getAbsoluteFile())
        snapshot != snapshotter.snapshot(new File("123"))
        snapshot != snapshotter.snapshot(new Bean(prop: "value2"))

        // Not subclasses of `File`
        snapshotter.snapshot(new TestFile("abc")) != snapshot
    }

    def "creates isolated file"() {
        expect:
        def isolated = snapshotter.isolate(new File("abc"))
        isolated instanceof FileValueSnapshot
        isolated.isolate() == new File("abc")
    }

    def "can coerce file value"() {
        expect:
        def isolated = snapshotter.isolate(new File("abc"))
        isolated.coerce(File).is(isolated)
        isolated.coerce(String) == null
    }

    def "creates snapshot for provider type"() {
        def value = Stub(Provider)
        value.get() >> "123"
        def value2 = Stub(Provider)
        value2.get() >> "123"
        def value3 = Stub(Provider)
        value3.get() >> "12"

        expect:
        def snapshot = snapshotter.snapshot(value)
        snapshot instanceof ProviderSnapshot
        snapshot == snapshotter.snapshot(value)
        snapshot == snapshotter.snapshot(value2)
        snapshot != snapshotter.snapshot(value3)
    }

    def "creates snapshot for named managed type"() {
        def instantiator = NamedObjectInstantiator.INSTANCE
        def value = instantiator.named(Thing, "value1")
        def value1 = instantiator.named(Thing, "value1")
        def value2 = instantiator.named(Thing, "value2")
        def value3 = instantiator.named(Named, "value1")

        expect:
        def snapshot = snapshotter.snapshot(value)
        snapshot instanceof ManagedNamedTypeSnapshot
        snapshot == snapshotter.snapshot(value)
        snapshot == snapshotter.snapshot(value1)
        snapshot != snapshotter.snapshot(value2)
        snapshot != snapshotter.snapshot(value3)
    }

    def "creates isolated named managed type"() {
        def instantiator = NamedObjectInstantiator.INSTANCE
        def value = instantiator.named(Thing, "value1")

        expect:
        def isolated = snapshotter.isolate(value)
        isolated instanceof ManagedNamedTypeSnapshot
        isolated.isolate() == value
    }

    def "can coerce named managed type"() {
        def instantiator = NamedObjectInstantiator.INSTANCE
        def value = instantiator.named(Thing, "value1")

        def spec = new FilteringClassLoader.Spec()
        spec.allowClass(Named)
        spec.allowPackage("org.gradle.api.internal.model") // mixed into the implementation
        def filter = new FilteringClassLoader(getClass().classLoader, spec)
        def loader = new GroovyClassLoader(filter)
        loader.addURL(ClasspathUtil.getClasspathForClass(GroovyObject).toURI().toURL())
        def cl = loader.parseClass("package ${Thing.package.name}; interface Thing extends ${Named.name} { }")
        assert cl != Thing
        assert Named.isAssignableFrom(cl)
        assert cl.name == Thing.name

        expect:
        def isolated = snapshotter.isolate(value)
        isolated.coerce(Thing).is(isolated)
        isolated.coerce(Named).is(isolated)
        isolated.coerce(String) == null

        def v = isolated.coerce(cl).isolate()
        cl.isInstance(v)
        v.name == "value1"
    }

    def "creates snapshot for serializable type"() {
        def value = new Bean()

        expect:
        def snapshot = snapshotter.snapshot(value)
        snapshot instanceof SerializedValueSnapshot
        snapshot == snapshotter.snapshot(value)
        snapshot == snapshotter.snapshot(new Bean())
        snapshot != snapshotter.snapshot(new Bean(prop: "value2"))
    }

    def "creates isolated serializable type"() {
        def value = new Bean(prop: "123")

        def loader = new GroovyClassLoader(getClass().classLoader)
        loader.addURL(ClasspathUtil.getClasspathForClass(GroovyObject).toURI().toURL())
        def cl = loader.parseClass("package ${Bean.package.name}; class Bean implements Serializable { String prop }")
        assert cl != Bean
        assert cl.name == Bean.name

        expect:
        def isolated = snapshotter.isolate(value)
        isolated instanceof SerializedValueSnapshot
        def other = isolated.isolate()
        other.prop == "123"
        !other.is(value)

        def v = isolated.coerce(cl).isolate()
        v.prop == "123"
    }

    def "can coerce serializable value"() {
        def value = new Bean(prop: "123")

        expect:
        def isolated = snapshotter.isolate(value)
        isolated.coerce(Bean).is(isolated)
        isolated.coerce(String) == null
    }

    def "creates snapshot for string from candidate"() {
        expect:
        def snapshot = snapshotter.snapshot("abc")
        areTheSame(snapshot, "abc")

        areNotTheSame(snapshot, "other")
        areNotTheSame(snapshot, 123L)
        areNotTheSame(snapshot, null)
        areNotTheSame(snapshot, new Bean())
    }

    def "creates snapshot for integer from candidate"() {
        expect:
        def snapshot = snapshotter.snapshot(123)
        areTheSame(snapshot, 123)

        areNotTheSame(snapshot, -12)
        areNotTheSame(snapshot, 123L)
        areNotTheSame(snapshot, 123 as short)
        areNotTheSame(snapshot, null)
        areNotTheSame(snapshot, new Bean())
    }

    def "creates snapshot for long from candidate"() {
        expect:
        def snapshot = snapshotter.snapshot(123L)
        areTheSame(snapshot, 123L)

        areNotTheSame(snapshot, -12L)
        areNotTheSame(snapshot, 123)
        areNotTheSame(snapshot, 123 as short)
        areNotTheSame(snapshot, null)
        areNotTheSame(snapshot, new Bean())
    }

    def "creates snapshot for short from candidate"() {
        expect:
        def snapshot = snapshotter.snapshot(123 as short)
        areTheSame(snapshot, 123 as short)

        areNotTheSame(snapshot, -12 as short)
        areNotTheSame(snapshot, 123)
        areNotTheSame(snapshot, 123L)
        areNotTheSame(snapshot, null)
        areNotTheSame(snapshot, new Bean())
    }

    def "creates snapshot for file from candidate"() {
        expect:
        def snapshot = snapshotter.snapshot(new File("abc"))
        areTheSame(snapshot, new File("abc"))

        areNotTheSame(snapshot, new File("other"))
        areNotTheSame(snapshot, new TestFile("abc"))
        areNotTheSame(snapshot, "abc")
        areNotTheSame(snapshot, 123)
        areNotTheSame(snapshot, null)
        areNotTheSame(snapshot, new Bean())
    }

    def "creates snapshot for enum from candidate"() {
        expect:
        def snapshot = snapshotter.snapshot(Type1.TWO)
        snapshotter.snapshot(Type1.TWO, snapshot).is(snapshot)

        snapshotter.snapshot(Type1.ONE, snapshot) != snapshot
        snapshotter.snapshot(Type1.ONE, snapshot) == snapshotter.snapshot(Type1.ONE)

        snapshotter.snapshot(Type2.TWO, snapshot) != snapshot
        snapshotter.snapshot(Type2.TWO, snapshot) == snapshotter.snapshot(Type2.TWO)

        snapshotter.snapshot(new Bean(), snapshot) != snapshot
        snapshotter.snapshot(new Bean(), snapshot) == snapshotter.snapshot(new Bean())
    }

    def "creates snapshot for null from candidate"() {
        expect:
        def snapshot = snapshotter.snapshot(null)
        snapshotter.snapshot(null, snapshot).is(snapshot)

        snapshotter.snapshot("other", snapshot) != snapshot
        snapshotter.snapshot("other", snapshot) == snapshotter.snapshot("other")

        snapshotter.snapshot(new Bean(), snapshot) != snapshot
        snapshotter.snapshot(new Bean(), snapshot) == snapshotter.snapshot(new Bean())
    }

    def "creates snapshot for boolean from candidate"() {
        expect:
        def snapshot = snapshotter.snapshot(true)
        snapshotter.snapshot(true, snapshot).is(snapshot)

        snapshotter.snapshot("other", snapshot) != snapshot
        snapshotter.snapshot("other", snapshot) == snapshotter.snapshot("other")

        snapshotter.snapshot(new Bean(), snapshot) != snapshot
        snapshotter.snapshot(new Bean(), snapshot) == snapshotter.snapshot(new Bean())
    }

    def "creates snapshot for array from candidate"() {
        expect:
        def snapshot1 = snapshotter.snapshot([] as Object[])
        snapshotter.snapshot([] as Object[], snapshot1).is(snapshot1)

        snapshotter.snapshot(["123"] as Object[], snapshot1) != snapshot1
        snapshotter.snapshot("other", snapshot1) != snapshot1
        snapshotter.snapshot(new Bean(), snapshot1) != snapshot1

        def snapshot2 = snapshotter.snapshot(["123"] as Object[])
        snapshotter.snapshot(["123"] as Object[], snapshot2).is(snapshot2)

        snapshotter.snapshot(["456"] as Object[], snapshot2) != snapshot2
        snapshotter.snapshot([] as Object[], snapshot2) != snapshot2
        snapshotter.snapshot(["123", "456"] as Object[], snapshot2) != snapshot2
    }

    def "creates snapshot for list from candidate"() {
        expect:
        def snapshot1 = snapshotter.snapshot([])
        snapshotter.snapshot([], snapshot1).is(snapshot1)

        snapshotter.snapshot(["123"], snapshot1) != snapshot1
        snapshotter.snapshot("other", snapshot1) != snapshot1
        snapshotter.snapshot(new Bean(), snapshot1) != snapshot1

        def snapshot2 = snapshotter.snapshot(["123"])
        snapshotter.snapshot(["123"], snapshot2).is(snapshot2)

        snapshotter.snapshot(["456"], snapshot2) != snapshot2
        snapshotter.snapshot([], snapshot2) != snapshot2
        snapshotter.snapshot(["123", "456"], snapshot2) != snapshot2

        def snapshot3 = snapshotter.snapshot([new Bean(prop: "value")])
        snapshotter.snapshot([new Bean(prop: "value")], snapshot3).is(snapshot3)

        snapshotter.snapshot([new Bean(prop: "value 2")], snapshot3) != snapshot3
        snapshotter.snapshot([], snapshot3) != snapshot3
        snapshotter.snapshot([new Bean(prop: "value"), new Bean(prop: "value")], snapshot3) != snapshot3

        def snapshot4 = snapshotter.snapshot([new Bean(prop: "value1"), new Bean(prop: "value2")])
        snapshotter.snapshot([new Bean(prop: "value1"), new Bean(prop: "value2")], snapshot4).is(snapshot4)
        snapshotter.snapshot([new Bean(prop: "value1"), new Bean(prop: "value3")], snapshot4) != snapshot4

        def snapshot5 = snapshotter.snapshot(["abc", "123"])
        def snapshot6 = snapshotter.snapshot(["abc", "123", "xyz"], snapshot5)
        snapshotter.snapshot(["abc", "123", "xyz"], snapshot6).is(snapshot6)
    }

    def "creates snapshot for set from candidates"() {
        expect:
        def snapshot1 = snapshotter.snapshot([] as Set)
        snapshotter.snapshot([] as Set, snapshot1).is(snapshot1)

        snapshotter.snapshot(["123"] as Set, snapshot1) != snapshot1
        snapshotter.snapshot("other", snapshot1) != snapshot1
        snapshotter.snapshot(new Bean(), snapshot1) != snapshot1

        def snapshot2 = snapshotter.snapshot(["123"] as Set)
        snapshotter.snapshot(["123"] as Set, snapshot2).is(snapshot2)

        snapshotter.snapshot(["123"], snapshot2) != snapshot2
        snapshotter.snapshot(["456"] as Set, snapshot2) != snapshot2
        snapshotter.snapshot([] as Set, snapshot2) != snapshot2
        snapshotter.snapshot(["123", "456"] as Set, snapshot2) != snapshot2

        def snapshot3 = snapshotter.snapshot([new Bean(prop: "value")] as Set)
        snapshotter.snapshot([new Bean(prop: "value")] as Set, snapshot3).is(snapshot3)

        snapshotter.snapshot([new Bean(prop: "value 2")] as Set, snapshot3) != snapshot3
        snapshotter.snapshot([] as Set, snapshot3) != snapshot3
        snapshotter.snapshot([new Bean(prop: "value 2"), new Bean(prop: "value")] as Set, snapshot3) != snapshot3
    }

    def "creates snapshot for map from candidate"() {
        def map1 = [:]
        map1.put(new Bean(prop: "value"), new Bean(prop: "value"))
        def map2 = [:]
        map2.put(new Bean(prop: "value"), new Bean(prop: "value2"))
        def map3 = [:]
        map3.putAll(map1)
        map3.put(new Bean(prop: "value2"), new Bean(prop: "value2"))

        expect:
        def snapshot1 = snapshotter.snapshot([:])
        snapshotter.snapshot([:], snapshot1).is(snapshot1)

        snapshotter.snapshot([12: "123"], snapshot1) != snapshot1

        snapshotter.snapshot("other", snapshot1) != snapshot1
        snapshotter.snapshot("other", snapshot1) == snapshotter.snapshot("other")
        snapshotter.snapshot(new Bean(), snapshot1) != snapshot1
        snapshotter.snapshot(new Bean(), snapshot1) == snapshotter.snapshot(new Bean())

        def snapshot2 = snapshotter.snapshot([12: "123"])
        snapshotter.snapshot([12: "123"], snapshot2).is(snapshot2)

        snapshotter.snapshot([12: "456"], snapshot2) != snapshot2
        snapshotter.snapshot([:], snapshot2) != snapshot2
        snapshotter.snapshot([123: "123"], snapshot2) != snapshot2
        snapshotter.snapshot([12: "123", 10: "123"], snapshot2) != snapshot2

        def snapshot3 = snapshotter.snapshot([a: new Bean(prop: "value")])
        snapshotter.snapshot([a: new Bean(prop: "value")], snapshot3).is(snapshot3)

        snapshotter.snapshot([a: new Bean(prop: "value 2")], snapshot3) != snapshot3
        snapshotter.snapshot([:], snapshot3) != snapshot3
        snapshotter.snapshot(map1, snapshot3) != snapshot3

        def snapshot4 = snapshotter.snapshot(map1)
        snapshotter.snapshot(map1, snapshot4).is(snapshot4)

        snapshotter.snapshot(map2, snapshot4) != snapshot4
        snapshotter.snapshot(map2, snapshot4) == snapshotter.snapshot(map2)
        snapshotter.snapshot(map3, snapshot4) != snapshot4
        snapshotter.snapshot(map3, snapshot4) == snapshotter.snapshot(map3)
    }

    def "creates snapshot for provider type from candidate"() {
        def value = Stub(Provider)
        value.get() >> "123"
        def value2 = Stub(Provider)
        value2.get() >> "123"
        def value3 = Stub(Provider)
        value3.get() >> "12"

        expect:
        def snapshot = snapshotter.snapshot(value)
        areTheSame(snapshot, value2)
        areNotTheSame(snapshot, value3)
        areNotTheSame(snapshot, "123")
    }

    def "creates snapshot for named managed type from candidate"() {
        def instantiator = NamedObjectInstantiator.INSTANCE
        def value = instantiator.named(Thing, "value")
        def value1 = instantiator.named(Thing, "value")
        def value2 = instantiator.named(Thing, "value2")
        def value3 = instantiator.named(Named, "value2")

        expect:
        def snapshot = snapshotter.snapshot(value)
        areTheSame(snapshot, value1)
        areNotTheSame(snapshot, value2)
        areNotTheSame(snapshot, value3)
        areNotTheSame(snapshot, "value")
    }

    def "creates snapshot for serializable type from candidate"() {
        expect:
        def snapshot = snapshotter.snapshot(new Bean(prop: "value"))
        snapshotter.snapshot(new Bean(prop: "value"), snapshot).is(snapshot)

        snapshotter.snapshot(new Bean(), snapshot) != snapshot
        snapshotter.snapshot(new Bean(), snapshot) == snapshotter.snapshot(new Bean())

        snapshotter.snapshot("other", snapshot) != snapshot
        snapshotter.snapshot("other", snapshot) == snapshotter.snapshot("other")
    }

    private void areTheSame(ValueSnapshot snapshot, Object value) {
        assert snapshotter.snapshot(value, snapshot).is(snapshot)
        assert snapshotter.snapshot(value, snapshot) == snapshotter.snapshot(value)
    }

    private void areNotTheSame(ValueSnapshot snapshot, Object value) {
        assert snapshotter.snapshot(value, snapshot) != snapshot
        assert snapshotter.snapshot(value) != snapshot
        def sn1 = snapshotter.snapshot(value, snapshot)
        def sn2 = snapshotter.snapshot(value)
        assert sn1 == sn2
    }
}
