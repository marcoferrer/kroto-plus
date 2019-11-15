plugins {
    `kotlin-dsl`
}
repositories {
    jcenter()
}

dependencies {
    // Needed by the script 'canteen.gradle'
    implementation("com.google.guava:guava:27.0.1-jre")
}