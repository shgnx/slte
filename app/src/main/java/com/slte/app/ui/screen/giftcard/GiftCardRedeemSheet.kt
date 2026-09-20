package com.slte.app.ui.screen.giftcard

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.slte.app.R
import com.slte.app.ui.component.AnimatedSticker
import com.slte.app.ui.component.SlteInput
import com.slte.app.ui.component.SlteInputSize
import com.slte.app.ui.component.SlteSubmitSheet
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.utils.Dimens
import com.slte.app.utils.Stickers

@Composable
fun GiftCardRedeemSheet(
    state: GiftCardRedeemState,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    SlteSubmitSheet(
        title = stringResource(R.string.gift_card_title),
        subtitle = stringResource(R.string.gift_card_subtitle),
        submitText = stringResource(R.string.gift_card_redeem),
        submitting = state.submitting,
        submitEnabled = state.code.isNotBlank(),
        onSubmit = onSubmit,
        onDismiss = onDismiss,
        header = {
            AnimatedSticker(
                assetPath = Stickers.GIFT_CARD,
                modifier = Modifier.size(Dimens.stateStickerSize),
            )
        },
    ) {
        SlteInput(
            value = state.code,
            onValueChange = onCodeChange,
            placeholder = stringResource(R.string.gift_card_code_hint),
            icon = SlteIcons.GiftCard,
            iconDesc = stringResource(R.string.gift_card_title),
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
            enabled = !state.submitting,
            size = SlteInputSize.Compact,
        )
    }
}
