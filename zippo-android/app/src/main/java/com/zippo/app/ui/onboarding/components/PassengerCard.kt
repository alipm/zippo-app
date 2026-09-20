package com.zippo.app.ui.onboarding.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zippo.app.data.PassengerResponse
import com.zippo.app.ui.theme.SurfaceGray
import com.zippo.app.ui.theme.TextMain
import com.zippo.app.ui.theme.YellowAuto

@Composable
fun PassengerCard(
    passenger: PassengerResponse,
    passengerBitmap: Bitmap?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceGray),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (passengerBitmap != null) {
                Image(
                    bitmap = passengerBitmap.asImageBitmap(),
                    contentDescription = "Passenger Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            YellowAuto,
                            CircleShape
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "👤",
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    passenger.name ?: "Subscriber",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextMain
                )
                Text(
                    "📱 Phone: ${passenger.phoneNumber} | Fare: ₹${passenger.monthlyFare}",
                    fontSize = 13.sp,
                    color = Color.LightGray
                )
                Text(
                    "📍 Sector: ${passenger.address}",
                    fontSize = 12.sp,
                    color = YellowAuto
                )
            }
            IconButton(onClick = onDelete) {
                Text("🗑️", fontSize = 20.sp)
            }
        }
    }
}