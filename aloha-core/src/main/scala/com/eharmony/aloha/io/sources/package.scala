package com.eharmony.aloha.io

import scala.annotation.implicitNotFound

package object sources {

    /** A type alias for a "''type class''" to convert of type ''A'' to a [[com.eharmony.aloha.io.sources.ReadableSource]].
      * @tparam A type of object to convert.
      */
    @implicitNotFound(
        msg = "Cannot find ReadableTypeConverter type class for ${A}.  Consider importing via " +
            "import com.eharmony.aloha.io.ReadableTypeConverters.Implicits._ and string " +
            "converters from com.eharmony.aloha.io.ReadableTypeConverters.StringImplicits")
    type ReadableSourceConverter[-A] = A => ReadableSource
}
