package com.example.fastingtracker.utils

data class FastingStatus(
    val current: String,
    val next: String
)

object FastingStatusProvider {
    private val statusMap = mapOf(
        1 to FastingStatus("Insulin is rising to process your last meal.", "Blood sugar will soon stabilize for storage."),
        2 to FastingStatus("Your body is in an anabolic (building) state.", "Absorption will continue as energy is stored."),
        3 to FastingStatus("Digestion is in full swing.", "Insulin levels will begin their gradual descent."),
        4 to FastingStatus("Nutrient absorption is peaking.", "Your body will soon shift from storing to using."),
        5 to FastingStatus("Blood glucose is returning to baseline.", "Glucagon will rise to maintain your energy."),
        6 to FastingStatus("The 'Metabolic Switch' is preparing to flip.", "Your liver will start releasing stored energy."),
        7 to FastingStatus("Insulin is reaching its lowest post-meal level.", "Fat-burning enzymes are becoming active."),
        8 to FastingStatus("Digestion is complete; you are post-absorptive.", "Glycogen stores will now be the primary fuel."),
        9 to FastingStatus("Liver glycogen is being tapped for energy.", "Your body will begin seeking fat for fuel."),
        10 to FastingStatus("Glucose levels are steady and controlled.", "Growth hormone will soon begin to rise."),
        11 to FastingStatus("Lipid metabolism (fat use) is accelerating.", "You are approaching the fat-burning zone."),
        12 to FastingStatus("You have reached the fat-burning threshold.", "Ketone production will begin in the liver."),
        13 to FastingStatus("Growth hormone is rising to protect muscle.", "Fat mobilization will significantly increase."),
        14 to FastingStatus("Your body is shifting into a catabolic state.", "Intracellular cleanup (autophagy) is near."),
        15 to FastingStatus("Fatty acids are now a primary energy source.", "Cellular repair processes are starting."),
        16 to FastingStatus("Autophagy is starting to ramp up.", "Your cells will begin clearing out waste."),
        17 to FastingStatus("Cellular 'recycling' is now active.", "Insulin sensitivity is improving rapidly."),
        18 to FastingStatus("Ketones are beginning to enter the blood.", "Brain fog may lift as BHB fuels neurons."),
        19 to FastingStatus("Fat oxidation is at a high steady state.", "Deeper cellular detoxification is occurring."),
        20 to FastingStatus("Inflammatory markers are beginning to drop.", "Your body is optimizing energy efficiency."),
        21 to FastingStatus("Autophagy is reaching peak effectiveness.", "Stem cell production may be stimulated."),
        22 to FastingStatus("Ketone levels are rising for brain fuel.", "Your metabolic flexibility is being tested."),
        23 to FastingStatus("Gluconeogenesis is fueling your vital organs.", "You are reaching a state of deep ketosis."),
        24 to FastingStatus("Cellular regeneration is at its 24hr peak.", "Your body is fully adapted to fasting.")
    )

    fun getStatusForHour(hour: Int): FastingStatus {
        // Returns the specific hour info, or a default if over 24 or at 0
        return statusMap[hour.coerceIn(1, 24)] ?: FastingStatus("Fast in progress...", "Keep going!")
    }
}