package com.hazlosano.data.product

import com.hazlosano.data.product.createProductDataSource
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.HazloSeller

object SeedProducts {

    val seller = HazloSeller(
        id = "05bea858-88d0-4ff3-a531-3d82a7ad6fcc",
        name = "Hazlo Sano",
        category = "Food",
        phone = "2781126948",
        url = "https://restaurante.hazlosano.com",
        description = "1. Sueno: Recuperacion biologica y descanso optimizado. 2. Alimentacion: Nutricion natural y conexion con el origen local. 3. Movimiento: Ejercicio funcional y actividad fisica constante. 4. Mente y Comunidad: Gestion emocional, proposito y conexion social.",
        logoUrl = "https://storage.googleapis.com/products_and_services/images/hazloSanoTextTransparencia_500x500.webp",
    )

    val products = listOf(
        HazloProduct(
            id = "077cacd9-5bca-4cfe-bbcb-93494d117fb0",
            name = "Pechuga de pollo a la naranja en bistec",
            price = 105.0,
            isAvailable = true,
            description = "Proteína de pollo magra para recuperación muscular y Grasas Buenas + Pan de masa madre con crema de cacahuate y fruta + Super Bowl de 18 ingredientes (Proteína vegetal, ligero y súper nutritivo. 18 ingredientes para tu microbiota, tu segundo cerebro.  INGREDIENTES: Nuez, almendras, arándanos, fresa, espinacas, lechuga, aguacate, zanahoria, pepino, jícama, jitomate, cebolla, manzana, uva, queso, ajonjolí negro, arroz integral, frijoles hervidos o refritos. Y el aderezo de tu preferencia).  Aderezos artesanales/caseros: - Dorado - Vinagreta balsámica, - Ranch         ",
            category = "Alimentación",
            subCategory = "Comidas",
            tags = listOf("vegetales", "frutos secos", "fruta", "fresa", "uva", "arroz", "frijol"),
            imageUrl = "https://storage.googleapis.com/products_and_services/images/00-2-pechuga-a-la-naranja.jpeg",
            productUrl = "https://wa.me/p/26114955168111668/5212781126948",
            sellerId = "05bea858-88d0-4ff3-a531-3d82a7ad6fcc",
        ),
        HazloProduct(
            id = "2a7b65d8-bb72-4a09-8227-e244b46d9fba",
            name = "Pechuga de pollo asada en bistec",
            price = 105.0,
            isAvailable = true,
            description = "Proteína de pollo magra para recuperación muscular y Grasas Buenas + Pan de masa madre con crema de cacahuate y fruta + Super Bowl de 18 ingredientes (Proteína vegetal, ligero y súper nutritivo. 18 ingredientes para tu microbiota, tu segundo cerebro.  INGREDIENTES: Nuez, almendras, arándanos, fresa, espinacas, lechuga, aguacate, zanahoria, pepino, jícama, jitomate, cebolla, manzana, uva, queso, ajonjolí negro, arroz integral, frijoles hervidos o refritos. Y el aderezo de tu preferencia).  Aderezos artesanales/caseros: - Dorado - Vinagreta balsámica, - Ranch         ",
            category = "Alimentación",
            subCategory = "Comidas",
            tags = listOf("proteína", "Proteína magra", "vegetales", "frutos secos", "fruta", "fresa", "uva", "arroz", "frijol"),
            imageUrl = "https://storage.googleapis.com/products_and_services/images/pechuga-asada.png",
            productUrl = "https://wa.me/p/25399356093059170/5212781126948",
            sellerId = "05bea858-88d0-4ff3-a531-3d82a7ad6fcc",
        ),
        HazloProduct(
            id = "4e256323-9965-5f82-a8d6-6fc2849e9c77",
            name = "Suero natural",
            price = 35.0,
            isAvailable = true,
            description = "Esta fórmula alcalinizante está creada para reponer cada mineral perdido por el calor o el esfuerzo intenso. Con una base de agua de coco, sal de mar y un toque de hierba buena, es el aliado perfecto para restaurar tu cuerpo de forma inteligente. Menos es más: solo lo que tus células necesitan para sanar y descansar.",
            category = "Alimentación",
            subCategory = "Bebidas",
            tags = listOf("agua de coco", "coco", "agua", "sal de mar", "miel", "limón"),
            imageUrl = "https://storage.googleapis.com/products_and_services/images/suero-natural.png",
            productUrl = "https://wa.me/5212781126948",
            sellerId = "05bea858-88d0-4ff3-a531-3d82a7ad6fcc",
        ),
        HazloProduct(
            id = "6ce8c9cd-c222-5579-8d84-0fe12fe59dd4",
            name = "Pechuga de pollo a la macha en bistec",
            price = 105.0,
            isAvailable = true,
            description = "Proteína de pollo magra para recuperación muscular y Grasas Buenas + Pan de masa madre con crema de cacahuate y fruta + Super Bowl de 18 ingredientes (Proteína vegetal, ligero y súper nutritivo. 18 ingredientes para tu microbiota, tu segundo cerebro.  INGREDIENTES: Nuez, almendras, arándanos, fresa, espinacas, lechuga, aguacate, zanahoria, pepino, jícama, jitomate, cebolla, manzana, uva, queso, ajonjolí negro, arroz integral, frijoles hervidos o refritos. Y el aderezo de tu preferencia).  Aderezos artesanales/caseros: - Dorado - Vinagreta balsámica, - Ranch",
            category = "Alimentación",
            subCategory = "Comidas",
            tags = listOf("proteina", "pollo", "vegetales", "frutos secos", "fruta", "fresa", "uva", "arroz", "frijol"),
            imageUrl = "https://storage.googleapis.com/products_and_services/images/00-3-pechuga-dulce-al-horno.jpeg",
            productUrl = "https://wa.me/p/26635172646075456/5212781126948",
            sellerId = "05bea858-88d0-4ff3-a531-3d82a7ad6fcc",
        ),
        HazloProduct(
            id = "6fc8a463-8a49-5004-bb98-6f60e46d01f2",
            name = "Eléctrolitos de frutos rojos",
            price = 35.0,
            isAvailable = true,
            description = "Diseñado para quienes no se detienen.  Es la alternativa natural para potenciar tu rendimiento sin químicos ni azúcares refinados. Una mezcla equilibrada de cítricos frescos, miel de abeja local y sales minerales que mantienen tus músculos en marcha. Energía pura que viene de la tierra para alimentar tu movimiento.",
            category = "Alimentación",
            subCategory = "Bebidas",
            tags = listOf("agua", "sal de mar", "panela", "miel", "fresa", "arandano", "Zarzamora", "limón"),
            imageUrl = "https://storage.googleapis.com/products_and_services/images/Gatorade%20natural%20de%20frutos%20rojos.jpeg",
            productUrl = "https://wa.me/5212781126948",
            sellerId = "05bea858-88d0-4ff3-a531-3d82a7ad6fcc",
        ),
        HazloProduct(
            id = "8aedb15d-05ef-5fe7-b3b1-adf4408764b9",
            name = "Agua de Avena con canela",
            price = 20.0,
            isAvailable = true,
            description = "Agua de avena con canela, endulzada con panela y miel",
            category = "Alimentación",
            subCategory = "Bebidas",
            tags = listOf("agua", "avena", "canela", "panela", "miel"),
            imageUrl = "https://storage.googleapis.com/products_and_services/images/agua-avena-y-canela.jpeg",
            productUrl = "https://wa.me/p/25566648192968461/5212781126948",
            sellerId = "05bea858-88d0-4ff3-a531-3d82a7ad6fcc",
        ),
        HazloProduct(
            id = "9bbad91b-eaf7-5322-beb8-dbe504ddfc8c",
            name = "Omelet con ensalada",
            price = 95.0,
            isAvailable = true,
            description = "Omelet cocinado con aceite de aguacate. Y incluidos a tu gusto y por el mismo precio; zanahoria, espinacas, calabacita y champiñones. (Proteína para recuperación muscular y Grasas Buenas). O huevos al gusto. + Pan de masa madre con crema de cacahuate y fruta + Super Bowl de 18 ingredientes (Proteína vegetal, ligero y súper nutritivo. 18 ingredientes para tu microbiota, tu segundo cerebro.  INGREDIENTES: Nuez, almendras, arándanos, fresa, espinacas, lechuga, aguacate, zanahoria, pepino, jícama, jitomate, cebolla, manzana, uva, queso, ajonjolí negro, arroz integral, frijoles hervidos o refritos. Y el aderezo de tu preferencia).  Aderezos artesanales/caseros: - Dorado - Vinagreta balsámica, - Ranch",
            category = "Alimentación",
            subCategory = "Comidas",
            tags = listOf("proteina", "huevo", "vegetales", "frutos secos", "fruta", "fresa", "uva", "arroz", "frijol"),
            imageUrl = "https://storage.googleapis.com/products_and_services/images/omelet-con-ensalada.png",
            productUrl = "https://wa.me/p/25467973129479836/5212781126948",
            sellerId = "05bea858-88d0-4ff3-a531-3d82a7ad6fcc",
        ),
        HazloProduct(
            id = "d70c8ffc-344e-514c-b052-3d1e0cf112fb",
            name = "Agua de piña con pepino",
            price = 20.0,
            isAvailable = true,
            description = "Agua de piña con pepino",
            category = "Alimentación",
            subCategory = "Bebidas",
            tags = listOf("agua", "piña", "pepino", "panela", "miel"),
            imageUrl = "https://storage.googleapis.com/products_and_services/images/agua-pi%C3%B1a-con-pepino.jpeg",
            productUrl = "https://wa.me/5212781126948",
            sellerId = "05bea858-88d0-4ff3-a531-3d82a7ad6fcc",
        ),
        HazloProduct(
            id = "f5258215-a56c-4c86-813e-89177f2860d2",
            name = "Jugo Verde",
            price = 40.0,
            isAvailable = true,
            description = "Jugo verde natural desintoxicante.",
            category = "Alimentación",
            subCategory = "Jugos",
            tags = listOf("verde", "salud", "detox"),
            imageUrl = "https://storage.googleapis.com/products_and_services/images/1-jugo-verde.jpeg",
            productUrl = "https://wa.me/p/25591063420533660/5212781126948",
            sellerId = "05bea858-88d0-4ff3-a531-3d82a7ad6fcc",
        )
    )

    suspend fun seedIfEmpty() {
        val ds = createProductDataSource()
        if (ds.count() > 0) return
        ds.saveSellers(listOf(seller))
        ds.saveProducts(products)
    }
}