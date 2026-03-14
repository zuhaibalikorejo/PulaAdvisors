package com.pula.surveysync.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pula.surveysync.data.local.dao.FarmerDao
import com.pula.surveysync.data.local.entity.FarmerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FarmerViewModel @Inject constructor(
    private val farmerDao: FarmerDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val farmers: StateFlow<List<FarmerEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                farmerDao.getAllActiveFarmers()
            } else {
                farmerDao.searchFarmers(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedFarmer = MutableStateFlow<FarmerEntity?>(null)
    val selectedFarmer: StateFlow<FarmerEntity?> = _selectedFarmer.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFarmer(farmer: FarmerEntity) {
        _selectedFarmer.value = farmer
    }

    fun clearSelection() {
        _selectedFarmer.value = null
    }

    // Helper function to pre-load sample farmers for testing
    fun loadSampleFarmers() {
        viewModelScope.launch {
            val sampleFarmers = listOf(
                FarmerEntity(
                    id = "farmer_1",
                    firstName = "Kofi",
                    lastName = "Mensah",
                    phoneNumber = "+233244567890",
                    village = "Aburi",
                    district = "Akuapem South",
                    region = "Eastern Region",
                    farmSize = 4.5,
                    farmSizeUnit = "acres",
                    registrationDate = java.util.Date(),
                    lastUpdated = java.util.Date(),
                    isActive = true,
                    notes = "Specializes in cocoa and plantain farming"
                ),
                FarmerEntity(
                    id = "farmer_2",
                    firstName = "Amina",
                    lastName = "Okonkwo",
                    phoneNumber = "+234803456789",
                    village = "Enugu-Ezike",
                    district = "Igbo-Eze North",
                    region = "Enugu State",
                    farmSize = 7.2,
                    farmSizeUnit = "acres",
                    registrationDate = java.util.Date(),
                    lastUpdated = java.util.Date(),
                    isActive = true,
                    notes = "Grows cassava, yam, and palm trees"
                ),
                FarmerEntity(
                    id = "farmer_3",
                    firstName = "Wanjiku",
                    lastName = "Kariuki",
                    phoneNumber = "+254722345678",
                    village = "Kiambu",
                    district = "Kiambu",
                    region = "Central Kenya",
                    farmSize = 3.8,
                    farmSizeUnit = "acres",
                    registrationDate = java.util.Date(),
                    lastUpdated = java.util.Date(),
                    isActive = true,
                    notes = "Coffee and tea farmer with irrigation"
                ),
                FarmerEntity(
                    id = "farmer_4",
                    firstName = "Thabo",
                    lastName = "Molefe",
                    phoneNumber = "+27823456789",
                    village = "Limpopo Valley",
                    district = "Makhado",
                    region = "Limpopo Province",
                    farmSize = 12.5,
                    farmSizeUnit = "acres",
                    registrationDate = java.util.Date(),
                    lastUpdated = java.util.Date(),
                    isActive = true,
                    notes = "Mixed farming: maize, vegetables, and livestock"
                ),
                FarmerEntity(
                    id = "farmer_5",
                    firstName = "Fatima",
                    lastName = "Diallo",
                    phoneNumber = "+221776543210",
                    village = "Thiès",
                    district = "Thiès Département",
                    region = "Thiès Region",
                    farmSize = 6.0,
                    farmSizeUnit = "acres",
                    registrationDate = java.util.Date(),
                    lastUpdated = java.util.Date(),
                    isActive = true,
                    notes = "Groundnut and millet production"
                ),
                FarmerEntity(
                    id = "farmer_6",
                    firstName = "Kwame",
                    lastName = "Nkrumah",
                    phoneNumber = "+233208765432",
                    village = "Kumasi",
                    district = "Kumasi Metropolitan",
                    region = "Ashanti Region",
                    farmSize = 9.3,
                    farmSizeUnit = "acres",
                    registrationDate = java.util.Date(),
                    lastUpdated = java.util.Date(),
                    isActive = true,
                    notes = "Organic cocoa farmer, Fair Trade certified"
                ),
                FarmerEntity(
                    id = "farmer_7",
                    firstName = "Aisha",
                    lastName = "Banda",
                    phoneNumber = "+265991234567",
                    village = "Lilongwe",
                    district = "Lilongwe District",
                    region = "Central Region",
                    farmSize = 5.7,
                    farmSizeUnit = "acres",
                    registrationDate = java.util.Date(),
                    lastUpdated = java.util.Date(),
                    isActive = true,
                    notes = "Tobacco and maize cultivation"
                ),
                FarmerEntity(
                    id = "farmer_8",
                    firstName = "Oluwaseun",
                    lastName = "Adeyemi",
                    phoneNumber = "+234809876543",
                    village = "Ibadan",
                    district = "Ibadan North",
                    region = "Oyo State",
                    farmSize = 11.0,
                    farmSizeUnit = "acres",
                    registrationDate = java.util.Date(),
                    lastUpdated = java.util.Date(),
                    isActive = true,
                    notes = "Cassava processing and palm oil production"
                ),
                FarmerEntity(
                    id = "farmer_9",
                    firstName = "Nandi",
                    lastName = "Muthoni",
                    phoneNumber = "+254733456789",
                    village = "Eldoret",
                    district = "Uasin Gishu",
                    region = "Rift Valley",
                    farmSize = 15.2,
                    farmSizeUnit = "acres",
                    registrationDate = java.util.Date(),
                    lastUpdated = java.util.Date(),
                    isActive = true,
                    notes = "Large-scale maize and wheat production"
                ),
                FarmerEntity(
                    id = "farmer_10",
                    firstName = "Mamadou",
                    lastName = "Traoré",
                    phoneNumber = "+226708765432",
                    village = "Bobo-Dioulasso",
                    district = "Houet Province",
                    region = "Hauts-Bassins",
                    farmSize = 4.2,
                    farmSizeUnit = "acres",
                    registrationDate = java.util.Date(),
                    lastUpdated = java.util.Date(),
                    isActive = true,
                    notes = "Cotton and sorghum farmer"
                )
            )
            farmerDao.insertAll(sampleFarmers)
        }
    }
}

