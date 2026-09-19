# Dynamic Material Island — app-level ProGuard/R8 rules.
#
# R8 is currently disabled (isMinifyEnabled = false); when release
# minification is enabled in a later milestone these rules apply.
#
# MediaController callbacks are registered at runtime; keep the inner
# callback class so R8 cannot strip it while the minified debug tooling
# (and future release builds) are in play.
-keep class com.howck.dmi.service.CapsuleService$* { *; }
