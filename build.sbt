import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
//import uk.gov.hmrc.ServiceManagerPlugin.Keys.itDependenciesList

lazy val appName: String = "two-way-message-frontend"

lazy val scalatest = "org.scalatest" %% "scalatest" % "3.0.5"


lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin, SbtArtifactory)
  .settings(DefaultBuildSettings.scalaSettings: _*)
  .settings(DefaultBuildSettings.defaultSettings(): _*)
  .settings(SbtDistributablesPlugin.publishingSettings: _*)
  .settings(majorVersion := 0)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(
    name := appName,
    RoutesKeys.routesImport ++= Seq("models._","controllers.binders.Binders._"),
    RoutesKeys.routesImport ++= Seq("models._"),
    PlayKeys.playDefaultPort := 8990,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*FrontendAuditConnector.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*LanguageSwitchController;.*.Binders.*;.*.views.*;.*.enquiry",
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    coverageExcludedPackages := "<empty>;Reverse.*;router\\.*",
    scalacOptions ++= Seq("-Xfatal-warnings", "-feature"),
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    evictionWarningOptions in update :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    fork in Test := true,
    resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo
    ),
    // concatenate js
    Concat.groups := Seq(
      "javascripts/twowaymessagefrontend-app.js" ->
        group(Seq("javascripts/show-hide-content.js", "javascripts/twowaymessagefrontend.js"))
    ),
    // prevent removal of unused code which generates warning errors due to use of third-party libs
    UglifyKeys.compressOptions := Seq("unused=false", "dead_code=false"),
    pipelineStages := Seq(digest),
    // below line required to force asset pipeline to operate in dev rather than only prod
    pipelineStages in Assets := Seq(concat,uglify),
    // only compress files generated by concat
    includeFilter in uglify := GlobFilter("twowaymessagefrontend-*.js")
  )
