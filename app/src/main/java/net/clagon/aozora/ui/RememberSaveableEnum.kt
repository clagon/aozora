package net.clagon.aozora.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * enum の選択を、画面回転やプロセスの再生成をまたいで残す。
 *
 * 既定の rememberSaveable は enum を Java のシリアライズで保存するので、enum の名前や置き場所を変えた版に
 * 更新したあとに前の状態を復元すると失敗する。ここでは名前の文字列で保存し、知らない名前なら initial に戻す。
 */
@Composable
inline fun <reified E : Enum<E>> rememberSaveableEnum(initial: E): MutableState<E> =
    rememberSaveable(
        saver = Saver<MutableState<E>, String>(
            save = { it.value.name },
            restore = { name -> mutableStateOf(enumValues<E>().firstOrNull { it.name == name } ?: initial) },
        ),
    ) { mutableStateOf(initial) }
