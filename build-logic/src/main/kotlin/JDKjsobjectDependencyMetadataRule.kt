import org.gradle.api.artifacts.*
import org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named
import org.gradle.nativeplatform.MachineArchitecture.ARCHITECTURE_ATTRIBUTE
import org.gradle.nativeplatform.OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE
import javax.inject.Inject

// Based on: https://github.com/gradlex-org/jvm-dependency-conflict-resolution/blob/main/src/main/java/org/gradlex/jvm/dependency/conflict/resolution/rules/AddTargetPlatformVariantsMetadataRule.java
@CacheableRule
abstract class JDKjsobjectDependencyMetadataRule @Inject constructor(
    private val classifier: String,
    private val operatingSystem: String,
    private val architecture: String,
    private val minJavaVersion: Int
) : ComponentMetadataRule {

    @get:Inject
    protected abstract val objects: ObjectFactory

    override fun execute(context: ComponentMetadataContext) {
        val details = context.details
        addTargetPlatformVariant(details, "Compile", "compile")
        addTargetPlatformVariant(details, "Runtime", "runtime")
    }

    private fun addTargetPlatformVariant(details: ComponentMetadataDetails, nameSuffix: String, baseVariant: String) {
        val name = details.id.name
        val version = details.id.version

        details.addVariant(classifier + nameSuffix + minJavaVersion, baseVariant) {
            configureAttributes()
            withFiles {
                removeAllFiles()
                addFile("$name-$version-$classifier.jar")
            }
            // depending on the JDK version, 'jsobject' is pulled in as extra dependency or not
            withDependencies {
                if (minJavaVersion >= 26) {
                    add("org.openjfx:jdk-jsobject")
                } else {
                    removeIf { it.name == "jdk-jsobject" }
                }
            }
        }
    }

    private fun VariantMetadata.configureAttributes() {
        attributes {
            attributes.attribute(OPERATING_SYSTEM_ATTRIBUTE, objects.named(operatingSystem))
            attributes.attribute(ARCHITECTURE_ATTRIBUTE, objects.named(architecture))
            attributes.attribute(TARGET_JVM_VERSION_ATTRIBUTE, minJavaVersion)
        }
    }
}
