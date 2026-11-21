package br.com.hotwheelscollector.data.di

import javax.inject.Qualifier

/** Provedor de código de barras (ML Kit `barcode-scanning`, T124). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class BarcodeRecognition

/** Provedor de OCR de embalagem (ML Kit `text-recognition`, T125). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class TextRecognition
