package de.tum.in.test.api.structural;

import static de.tum.in.test.api.localization.Messages.*;
import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.json.JSONArray;
import org.junit.jupiter.api.*;

/**
 * This test evaluates if the specified attributes in the structure oracle are
 * correctly implemented with the expected type, visibility modifiers and
 * annotations, based on its definition in the structure oracle (test.json).
 *
 * @author Stephan Krusche (krusche@in.tum.de)
 * @version 5.1 (2022-03-30)
 */
@API(status = Status.STABLE)
public abstract class AttributeTestProvider extends StructuralTestProvider {

	/**
	 * This method collects the classes in the structure oracle file for which
	 * attributes are specified. These classes are then transformed into JUnit 5
	 * dynamic tests.
	 *
	 * @return A dynamic test container containing the test for each class which is
	 *         then executed by JUnit.
	 * @throws URISyntaxException an exception if the URI of the class name cannot
	 *                            be generated (which seems to be unlikely)
	 */
	protected DynamicContainer generateTestsForAllClasses() throws URISyntaxException {
		List<DynamicNode> tests = new ArrayList<>();
		if (structureOracleJSON == null)
			throw failure(
					"The AttributeTest test can only run if the structural oracle (test.json) is present. If you do not provide it, delete AttributeTest.java!"); //$NON-NLS-1$
		for (var i = 0; i < structureOracleJSON.length(); i++) {
			var expectedClassJSON = structureOracleJSON.getJSONObject(i);
			// Only test the classes that have attributes defined in the oracle.
			if (expectedClassJSON.has(JSON_PROPERTY_CLASS) && (expectedClassJSON.has(JSON_PROPERTY_ATTRIBUTES)
					|| expectedClassJSON.has(JSON_PROPERTY_ENUM_VALUES))) {
				var expectedClassPropertiesJSON = expectedClassJSON.getJSONObject(JSON_PROPERTY_CLASS);
				var expectedClassName = expectedClassPropertiesJSON.getString(JSON_PROPERTY_NAME);
				var expectedPackageName = expectedClassPropertiesJSON.getString(JSON_PROPERTY_PACKAGE);
				var expectedClassStructure = new ExpectedClassStructure(expectedClassName, expectedPackageName,
						expectedClassJSON);
				tests.add(dynamicTest("testAttributes[" + expectedClassName + "]", //$NON-NLS-1$ //$NON-NLS-2$
						() -> testAttributes(expectedClassStructure)));
			}
		}
		if (tests.isEmpty())
			throw failure(
					"No tests for attributes available in the structural oracle (test.json). Either provide attributes information or delete AttributeTest.java!"); //$NON-NLS-1$
		/*
		 * Using a custom URI here to workaround surefire rendering the JUnit XML
		 * without the correct test names.
		 */
		return dynamicContainer(getClass().getName(), new URI(getClass().getName()), tests.stream());
	}

	/**
	 * This method gets passed the expected class structure generated by the method
	 * {@link #generateTestsForAllClasses()}, checks if the class is found at all in
	 * the assignment and then proceeds to check its attributes.
	 *
	 * @param expectedClassStructure The class structure that we expect to find and
	 *                               test against.
	 */
	protected static void testAttributes(ExpectedClassStructure expectedClassStructure) {
		var expectedClassName = expectedClassStructure.getExpectedClassName();
		var observedClass = findClassForTestType(expectedClassStructure, "attribute"); //$NON-NLS-1$
		if (expectedClassStructure.hasProperty(JSON_PROPERTY_ATTRIBUTES)) {
			var expectedAttributes = expectedClassStructure.getPropertyAsJsonArray(JSON_PROPERTY_ATTRIBUTES);
			checkAttributes(expectedClassName, observedClass, expectedAttributes);
		}
		if (expectedClassStructure.hasProperty(JSON_PROPERTY_ENUM_VALUES)) {
			var expectedEnumValues = expectedClassStructure.getPropertyAsJsonArray(JSON_PROPERTY_ENUM_VALUES);
			checkEnumValues(expectedClassName, observedClass, expectedEnumValues);
		}
	}

	/**
	 * This method checks if a observed class' attributes match the expected ones
	 * defined in the structure oracle.
	 *
	 * @param expectedClassName  The simple name of the class, mainly used for error
	 *                           messages.
	 * @param observedClass      The class that needs to be checked as a Class
	 *                           object.
	 * @param expectedAttributes The information on the expected attributes
	 *                           contained in a JSON array. This information
	 *                           consists of the name, the type and the visibility
	 *                           modifiers of each attribute.
	 */
	protected static void checkAttributes(String expectedClassName, Class<?> observedClass,
			JSONArray expectedAttributes) {
		for (var i = 0; i < expectedAttributes.length(); i++) {
			var expectedAttribute = expectedAttributes.getJSONObject(i);
			var expectedName = expectedAttribute.getString(JSON_PROPERTY_NAME);
			var expectedTypeName = expectedAttribute.getString(JSON_PROPERTY_TYPE);
			var expectedModifiers = getExpectedJsonProperty(expectedAttribute, JSON_PROPERTY_MODIFIERS);
			var expectedAnnotations = getExpectedJsonProperty(expectedAttribute, JSON_PROPERTY_ANNOTATIONS);

			// We check for each expected attribute if the name and the type is right.
			var nameIsCorrect = false;
			var typeIsCorrect = false;
			var modifiersAreCorrect = false;
			var annotationsAreCorrect = false;

			for (Field observedAttribute : observedClass.getDeclaredFields()) {
				if (expectedName.equals(observedAttribute.getName())) {
					nameIsCorrect = true;
					typeIsCorrect = checkExpectedType(observedAttribute.getType(), observedAttribute.getGenericType(),
							expectedTypeName);
					modifiersAreCorrect = checkModifiers(Modifier.toString(observedAttribute.getModifiers()).split(" "), //$NON-NLS-1$
							expectedModifiers);
					annotationsAreCorrect = checkAnnotations(observedAttribute.getAnnotations(), expectedAnnotations);
					// If all are correct, then we found our attribute and we can break the loop
					if (typeIsCorrect && modifiersAreCorrect && annotationsAreCorrect)
						break;
				}
				// TODO: we should also take wrong case and typos into account (the else case)
			}
			checkAttributeCorrectness(nameIsCorrect, typeIsCorrect, modifiersAreCorrect, annotationsAreCorrect,
					expectedName, expectedClassName);
		}
	}

	private static void checkAttributeCorrectness(boolean nameIsCorrect, boolean typeIsCorrect,
			boolean modifiersAreCorrect, boolean annotationsAreCorrect, String expectedName, String expectedClassName) {
		if (!nameIsCorrect)
			throw localizedFailure("structural.attribute.name", expectedName, expectedClassName); //$NON-NLS-1$
		if (!typeIsCorrect)
			throw localizedFailure("structural.attribute.type", expectedName, expectedClassName); //$NON-NLS-1$
		if (!modifiersAreCorrect)
			throw localizedFailure("structural.attribute.modifiers", expectedName, expectedClassName); //$NON-NLS-1$
		if (!annotationsAreCorrect)
			throw localizedFailure("structural.attribute.annotations", expectedName, expectedClassName); //$NON-NLS-1$
	}

	/**
	 * This method checks if the observed enum values match the expected ones
	 * defined in the structure oracle.
	 *
	 * @param expectedClassName  The simple name of the class, mainly used for error
	 *                           messages.
	 * @param observedClass      The enum that needs to be checked as a Class
	 *                           object.
	 * @param expectedEnumValues The information on the expected enum values
	 *                           contained in a JSON array. This information
	 *                           consists of the name of each enum value.
	 */
	protected static void checkEnumValues(String expectedClassName, Class<?> observedClass,
			JSONArray expectedEnumValues) {
		if (!observedClass.isEnum())
			throw localizedFailure("structural.attribute.noEnumConstants", expectedClassName); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		var observedEnumValues = ((Class<? extends Enum<?>>) observedClass).getEnumConstants();
		var observedEnumNames = Stream.of(observedEnumValues).map(Enum::name).collect(Collectors.toSet());
		var expectedEnumNames = IntStream.range(0, expectedEnumValues.length()).mapToObj(expectedEnumValues::getString)
				.collect(Collectors.toSet());
		var missing = expectedEnumNames.stream().filter(not(observedEnumNames::contains)).findFirst();
		missing.ifPresent(missingName -> fail(
				localized("structural.attribute.missingEnumConstants", expectedClassName, missingName))); //$NON-NLS-1$
		var unexpected = observedEnumNames.stream().filter(not(expectedEnumNames::contains)).findFirst();
		unexpected.ifPresent(unexpectedName -> fail(
				localized("structural.attribute.unexpectedEnumConstants", expectedClassName, unexpectedName))); //$NON-NLS-1$
	}
}
