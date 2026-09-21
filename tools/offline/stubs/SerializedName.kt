// Compile-only annotation shape, not Gson serialization.
package com.google.gson.annotations
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class SerializedName(val value: String)
