package com.slte.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun GiftCardRedeemHost(viewModels: LoggedInViewModels) {
    LaunchedEffect(Unit) {
        viewModels.giftCard.redeemed.collect {
            viewModels.giftCard.ackRedeemed()
            viewModels.profile.refresh(force = true)
            viewModels.main.refresh(force = true)
            viewModels.server.loadNodes(force = true)
        }
    }
}
