package de.tum.cit.ase.ares.api.architecturetest.java.postcompile;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import de.tum.cit.ase.ares.api.architecturetest.java.FileHandlerConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * This class runs the security rules on the architecture for the post-compile mode.
 */
public class JavaArchitectureTestCaseCollection {

    private JavaArchitectureTestCaseCollection() {
        throw new IllegalArgumentException("This class should not be instantiated");
    }

    /**
     * Error message for when the forbidden methods could not be loaded from the file
     */
    public static final String LOAD_FORBIDDEN_METHODS_FROM_FILE_FAILED = "Could not load the architecture rule file content";

    /**
     * Load forbidden methods from a file
     */
    public static Set<String> getForbiddenMethods(Path filePath) throws IOException {
        return new HashSet<>(Files.readAllLines(filePath));
    }

    /**
     * Get the content of a file from the architectural rules storage
     */
    public static String getArchitectureRuleFileContent(String key) throws IOException {
        return Files.readString(Paths.get("src", "main", "resources", "archunit", "files", "java", "rules", "%s.txt".formatted(key)));
    }

    /**
     * This method checks if any class in the given package accesses the file system.
     */
    public static final ArchRule NO_CLASS_SHOULD_ACCESS_FILE_SYSTEM = ArchRuleDefinition.noClasses()
            .should(new TransitivelyAccessesMethodsCondition(new DescribedPredicate<>("accesses file system") {
                private Set<String> forbiddenMethods;

                @Override
                public boolean test(JavaAccess<?> javaAccess) {
                    if (forbiddenMethods == null) {
                        try {
                            forbiddenMethods = getForbiddenMethods(FileHandlerConstants.JAVA_FILESYSTEM_INTERACTION_METHODS);
                        } catch (IOException e) {
                            throw new IllegalStateException(LOAD_FORBIDDEN_METHODS_FROM_FILE_FAILED, e);
                        }
                    }

                    return forbiddenMethods.stream().anyMatch(method -> javaAccess.getTarget().getFullName().startsWith(method));
                }
            }));

    /**
     * This method checks if any class in the given package accesses the network.
     */
    public static final ArchRule NO_CLASSES_SHOULD_ACCESS_NETWORK = ArchRuleDefinition.noClasses()
            .should(new TransitivelyAccessesMethodsCondition(new DescribedPredicate<>("accesses network") {
                private Set<String> forbiddenMethods;

                @Override
                public boolean test(JavaAccess<?> javaAccess) {
                    if (forbiddenMethods == null) {
                        try {
                            forbiddenMethods = getForbiddenMethods(FileHandlerConstants.JAVA_NETWORK_ACCESS_METHODS);
                        } catch (IOException e) {
                            throw new IllegalStateException(LOAD_FORBIDDEN_METHODS_FROM_FILE_FAILED, e);
                        }
                    }

                    return forbiddenMethods.stream().anyMatch(method -> javaAccess.getTarget().getFullName().startsWith(method));
                }
            }));

    /**
     * This method checks if any class in the given package imports forbidden packages.
     */
    public static ArchRule noClassesShouldImportForbiddenPackages(Set<String> allowedPackages) {
        return ArchRuleDefinition.noClasses()
                .should()
                .transitivelyDependOnClassesThat(new DescribedPredicate<>("imports package") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        return !allowedPackages.contains(javaClass.getPackageName());
                    }
                });
    }

    /**
     * This method checks if any class in the given package uses reflection.
     */
    public static final ArchRule NO_CLASSES_SHOULD_USE_REFLECTION = ArchRuleDefinition.noClasses()
            .should(new TransitivelyAccessesMethodsCondition(new DescribedPredicate<>("uses reflection") {
                private Set<String> forbiddenMethods;

                @Override
                public boolean test(JavaAccess<?> javaAccess) {
                    if (forbiddenMethods == null) {
                        try {
                            forbiddenMethods = getForbiddenMethods(FileHandlerConstants.JAVA_REFLECTION_METHODS);
                        } catch (IOException e) {
                            throw new IllegalStateException(LOAD_FORBIDDEN_METHODS_FROM_FILE_FAILED, e);
                        }
                    }

                    return forbiddenMethods.stream().anyMatch(method -> javaAccess.getTarget().getFullName().startsWith(method));
                }
            }));

    /**
     * This method checks if any class in the given package uses the command line.
     */
    public static final ArchRule NO_CLASSES_SHOULD_TERMINATE_JVM = ArchRuleDefinition.noClasses()
            .should(new TransitivelyAccessesMethodsCondition((new DescribedPredicate<>("terminates JVM") {
                private Set<String> forbiddenMethods;

                @Override
                public boolean test(JavaAccess<?> javaAccess) {
                    if (forbiddenMethods == null) {
                        try {
                            forbiddenMethods = getForbiddenMethods(FileHandlerConstants.JAVA_JVM_TERMINATION_METHODS);
                        } catch (IOException e) {
                            throw new IllegalStateException(LOAD_FORBIDDEN_METHODS_FROM_FILE_FAILED, e);
                        }
                    }

                    return forbiddenMethods.stream().anyMatch(method -> javaAccess.getTarget().getFullName().startsWith(method));
                }
            })));

    public static final ArchRule NO_CLASSES_SHOULD_EXECUTE_COMMANDS = ArchRuleDefinition.noClasses()
            .should(new TransitivelyAccessesMethodsCondition(new DescribedPredicate<>("executes commands") {
                private Set<String> forbiddenMethods;

                @Override
                public boolean test(JavaAccess<?> javaAccess) {
                    if (forbiddenMethods == null) {
                        try {
                            forbiddenMethods = getForbiddenMethods(FileHandlerConstants.JAVA_COMMAND_EXECUTION_METHODS);
                        } catch (IOException e) {
                            throw new IllegalStateException(LOAD_FORBIDDEN_METHODS_FROM_FILE_FAILED, e);
                        }
                    }

                    return forbiddenMethods.stream().anyMatch(method -> javaAccess.getTarget().getFullName().startsWith(method));
                }
            }));
}