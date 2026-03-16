dependencies {
    implementation(project(":api"))
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    // SQLite driver (shaded in)
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    // HikariCP for connection pooling
    implementation("com.zaxxer:HikariCP:5.1.0")
    // PlaceholderAPI (soft depend — not shaded)
    compileOnly("me.clip:placeholderapi:2.11.6")
}
