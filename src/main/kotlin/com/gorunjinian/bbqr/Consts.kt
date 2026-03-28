package com.gorunjinian.bbqr

internal const val HEADER_LENGTH = 8

const val MAX_PARTS = 1295

// Taken from pyqrcode tables.
// Indexed by: [version 0-39][error correction level 0-3][encoding 0-4]
// Encoding indices: 0=TotalBits, 1=Numeric, 2=Alphanumeric, 3=Byte, 4=Kanji
internal val QR_DATA_CAPACITY: Array<Array<IntArray>> = arrayOf(
    arrayOf(intArrayOf(152, 41, 25, 17, 10), intArrayOf(128, 34, 20, 14, 8), intArrayOf(104, 27, 16, 11, 7), intArrayOf(72, 17, 10, 7, 4)),
    arrayOf(intArrayOf(272, 77, 47, 32, 20), intArrayOf(224, 63, 38, 26, 16), intArrayOf(176, 48, 29, 20, 12), intArrayOf(128, 34, 20, 14, 8)),
    arrayOf(intArrayOf(440, 127, 77, 53, 32), intArrayOf(352, 101, 61, 42, 26), intArrayOf(272, 77, 47, 32, 20), intArrayOf(208, 58, 35, 24, 15)),
    arrayOf(intArrayOf(640, 187, 114, 78, 48), intArrayOf(512, 149, 90, 62, 38), intArrayOf(384, 111, 67, 46, 28), intArrayOf(288, 82, 50, 34, 21)),
    arrayOf(intArrayOf(864, 255, 154, 106, 65), intArrayOf(688, 202, 122, 84, 52), intArrayOf(496, 144, 87, 60, 37), intArrayOf(368, 106, 64, 44, 27)),
    arrayOf(intArrayOf(1088, 322, 195, 134, 82), intArrayOf(864, 255, 154, 106, 65), intArrayOf(608, 178, 108, 74, 45), intArrayOf(480, 139, 84, 58, 36)),
    arrayOf(intArrayOf(1248, 370, 224, 154, 95), intArrayOf(992, 293, 178, 122, 75), intArrayOf(704, 207, 125, 86, 53), intArrayOf(528, 154, 93, 64, 39)),
    arrayOf(intArrayOf(1552, 461, 279, 192, 118), intArrayOf(1232, 365, 221, 152, 93), intArrayOf(880, 259, 157, 108, 66), intArrayOf(688, 202, 122, 84, 52)),
    arrayOf(intArrayOf(1856, 552, 335, 230, 141), intArrayOf(1456, 432, 262, 180, 111), intArrayOf(1056, 312, 189, 130, 80), intArrayOf(800, 235, 143, 98, 60)),
    arrayOf(intArrayOf(2192, 652, 395, 271, 167), intArrayOf(1728, 513, 311, 213, 131), intArrayOf(1232, 364, 221, 151, 93), intArrayOf(976, 288, 174, 119, 74)),
    arrayOf(intArrayOf(2592, 772, 468, 321, 198), intArrayOf(2032, 604, 366, 251, 155), intArrayOf(1440, 427, 259, 177, 109), intArrayOf(1120, 331, 200, 137, 85)),
    arrayOf(intArrayOf(2960, 883, 535, 367, 226), intArrayOf(2320, 691, 419, 287, 177), intArrayOf(1648, 489, 296, 203, 125), intArrayOf(1264, 374, 227, 155, 96)),
    arrayOf(intArrayOf(3424, 1022, 619, 425, 262), intArrayOf(2672, 796, 483, 331, 204), intArrayOf(1952, 580, 352, 241, 149), intArrayOf(1440, 427, 259, 177, 109)),
    arrayOf(intArrayOf(3688, 1101, 667, 458, 282), intArrayOf(2920, 871, 528, 362, 223), intArrayOf(2088, 621, 376, 258, 159), intArrayOf(1576, 468, 283, 194, 120)),
    arrayOf(intArrayOf(4184, 1250, 758, 520, 320), intArrayOf(3320, 991, 600, 412, 254), intArrayOf(2360, 703, 426, 292, 180), intArrayOf(1784, 530, 321, 220, 136)),
    arrayOf(intArrayOf(4712, 1408, 854, 586, 361), intArrayOf(3624, 1082, 656, 450, 277), intArrayOf(2600, 775, 470, 322, 198), intArrayOf(2024, 602, 365, 250, 154)),
    arrayOf(intArrayOf(5176, 1548, 938, 644, 397), intArrayOf(4056, 1212, 734, 504, 310), intArrayOf(2936, 876, 531, 364, 224), intArrayOf(2264, 674, 408, 280, 173)),
    arrayOf(intArrayOf(5768, 1725, 1046, 718, 442), intArrayOf(4504, 1346, 816, 560, 345), intArrayOf(3176, 948, 574, 394, 243), intArrayOf(2504, 746, 452, 310, 191)),
    arrayOf(intArrayOf(6360, 1903, 1153, 792, 488), intArrayOf(5016, 1500, 909, 624, 384), intArrayOf(3560, 1063, 644, 442, 272), intArrayOf(2728, 813, 493, 338, 208)),
    arrayOf(intArrayOf(6888, 2061, 1249, 858, 528), intArrayOf(5352, 1600, 970, 666, 410), intArrayOf(3880, 1159, 702, 482, 297), intArrayOf(3080, 919, 557, 382, 235)),
    arrayOf(intArrayOf(7456, 2232, 1352, 929, 572), intArrayOf(5712, 1708, 1035, 711, 438), intArrayOf(4096, 1224, 742, 509, 314), intArrayOf(3248, 969, 587, 403, 248)),
    arrayOf(intArrayOf(8048, 2409, 1460, 1003, 618), intArrayOf(6256, 1872, 1134, 779, 480), intArrayOf(4544, 1358, 823, 565, 348), intArrayOf(3536, 1056, 640, 439, 270)),
    arrayOf(intArrayOf(8752, 2620, 1588, 1091, 672), intArrayOf(6880, 2059, 1248, 857, 528), intArrayOf(4912, 1468, 890, 611, 376), intArrayOf(3712, 1108, 672, 461, 284)),
    arrayOf(intArrayOf(9392, 2812, 1704, 1171, 721), intArrayOf(7312, 2188, 1326, 911, 561), intArrayOf(5312, 1588, 963, 661, 407), intArrayOf(4112, 1228, 744, 511, 315)),
    arrayOf(intArrayOf(10208, 3057, 1853, 1273, 784), intArrayOf(8000, 2395, 1451, 997, 614), intArrayOf(5744, 1718, 1041, 715, 440), intArrayOf(4304, 1286, 779, 535, 330)),
    arrayOf(intArrayOf(10960, 3283, 1990, 1367, 842), intArrayOf(8496, 2544, 1542, 1059, 652), intArrayOf(6032, 1804, 1094, 751, 462), intArrayOf(4768, 1425, 864, 593, 365)),
    arrayOf(intArrayOf(11744, 3514, 2132, 1465, 902), intArrayOf(9024, 2701, 1637, 1125, 692), intArrayOf(6464, 1933, 1172, 805, 496), intArrayOf(5024, 1501, 910, 625, 385)),
    arrayOf(intArrayOf(12248, 3669, 2223, 1528, 940), intArrayOf(9544, 2857, 1732, 1190, 732), intArrayOf(6968, 2085, 1263, 868, 534), intArrayOf(5288, 1581, 958, 658, 405)),
    arrayOf(intArrayOf(13048, 3909, 2369, 1628, 1002), intArrayOf(10136, 3035, 1839, 1264, 778), intArrayOf(7288, 2181, 1322, 908, 559), intArrayOf(5608, 1677, 1016, 698, 430)),
    arrayOf(intArrayOf(13880, 4158, 2520, 1732, 1066), intArrayOf(10984, 3289, 1994, 1370, 843), intArrayOf(7880, 2358, 1429, 982, 604), intArrayOf(5960, 1782, 1080, 742, 457)),
    arrayOf(intArrayOf(14744, 4417, 2677, 1840, 1132), intArrayOf(11640, 3486, 2113, 1452, 894), intArrayOf(8264, 2473, 1499, 1030, 634), intArrayOf(6344, 1897, 1150, 790, 486)),
    arrayOf(intArrayOf(15640, 4686, 2840, 1952, 1201), intArrayOf(12328, 3693, 2238, 1538, 947), intArrayOf(8920, 2670, 1618, 1112, 684), intArrayOf(6760, 2022, 1226, 842, 518)),
    arrayOf(intArrayOf(16568, 4965, 3009, 2068, 1273), intArrayOf(13048, 3909, 2369, 1628, 1002), intArrayOf(9368, 2805, 1700, 1168, 719), intArrayOf(7208, 2157, 1307, 898, 553)),
    arrayOf(intArrayOf(17528, 5253, 3183, 2188, 1347), intArrayOf(13800, 4134, 2506, 1722, 1060), intArrayOf(9848, 2949, 1787, 1228, 756), intArrayOf(7688, 2301, 1394, 958, 590)),
    arrayOf(intArrayOf(18448, 5529, 3351, 2303, 1417), intArrayOf(14496, 4343, 2632, 1809, 1113), intArrayOf(10288, 3081, 1867, 1283, 790), intArrayOf(7888, 2361, 1431, 983, 605)),
    arrayOf(intArrayOf(19472, 5836, 3537, 2431, 1496), intArrayOf(15312, 4588, 2780, 1911, 1176), intArrayOf(10832, 3244, 1966, 1351, 832), intArrayOf(8432, 2524, 1530, 1051, 647)),
    arrayOf(intArrayOf(20528, 6153, 3729, 2563, 1577), intArrayOf(15936, 4775, 2894, 1989, 1224), intArrayOf(11408, 3417, 2071, 1423, 876), intArrayOf(8768, 2625, 1591, 1093, 673)),
    arrayOf(intArrayOf(21616, 6479, 3927, 2699, 1661), intArrayOf(16816, 5039, 3054, 2099, 1292), intArrayOf(12016, 3599, 2181, 1499, 923), intArrayOf(9136, 2735, 1658, 1139, 701)),
    arrayOf(intArrayOf(22496, 6743, 4087, 2809, 1729), intArrayOf(17728, 5313, 3220, 2213, 1362), intArrayOf(12656, 3791, 2298, 1579, 972), intArrayOf(9776, 2927, 1774, 1219, 750)),
    arrayOf(intArrayOf(23648, 7089, 4296, 2953, 1817), intArrayOf(18672, 5596, 3391, 2331, 1435), intArrayOf(13328, 3993, 2420, 1663, 1024), intArrayOf(10208, 3057, 1852, 1273, 784)),
)
