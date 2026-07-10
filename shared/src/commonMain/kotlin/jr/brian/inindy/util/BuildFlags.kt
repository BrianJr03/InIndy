package jr.brian.inindy.util

/**
 * True when the app is running in a debug build. Used to gate log severity in
 * [initAppLogging] — never call this in feature code to branch behavior.
 *
 * Android reads from `BuildConfig.DEBUG` via a generated constants file (see
 * shared/build.gradle.kts). iOS reads `DEBUG` via a preprocessor macro in
 * the shared framework. JS / wasmJs default to `true` since those targets are
 * disabled for MVP and only run locally.
 */
expect val isDebugBuild: Boolean
