package com.example.japantripmap

/**
 * 観光スポット写真の帰属情報（フォトクレジット）。
 *
 * iOS 版 Settings.bundle/PhotoCredits.plist の内容をそのまま移植。
 * iOS では OS の設定アプリ内にクレジットを表示するが、Android には
 * 相当する仕組みが無いため、アプリ内の [PhotoCreditsScreen] で表示する。
 *
 * CC BY ライセンスの作品は帰属表示が法的に必須なので、この文言は
 * 削除・改変しないこと。CC0 / パブリックドメインは義務ではないが、
 * iOS 版に合わせて参考出典として併記している。
 */
const val PHOTO_CREDITS_TEXT: String = """本アプリの観光スポット写真の一部は、Wikimedia Commons 上のクリエイティブ・コモンズ表示（CC BY）ライセンスで提供されている作品を、アプリ内表示に合わせてリサイズして使用しています。各作品の帰属情報は以下の通りです。

──────────────
CC BY（表示ライセンス）
──────────────
● 青島
  「Ogre's Washboards and Torii, Aoshima, Miyazaki - Nov 5, 2017.jpg」
  Author: Cher V
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Ogre%27s_Washboards_and_Torii,_Aoshima,_Miyazaki_-_Nov_5,_2017.jpg
  ※アプリ表示用にリサイズ (resized)

● 熱田神宮
  「Atsuta-jingū (Atsuta-ku, Nagoya) VLux5 hdsr at10.jpg」
  Author: Hyppolyte de Saint-Rambert
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Atsuta-jing%C5%AB_(Atsuta-ku,_Nagoya)_VLux5_hdsr_at10.jpg
  ※アプリ表示用にリサイズ (resized)

● キャナルシティ
  「Canal city hakata fukuoka3.jpg」
  Author: FlyingToaster
  License: CC BY 3.0 (https://creativecommons.org/licenses/by/3.0)
  Source: https://commons.wikimedia.org/wiki/File:Canal_city_hakata_fukuoka3.jpg
  ※アプリ表示用にリサイズ (resized)

● 秩父
  「Chichibu's shibazakura, Saitama Prefecture; April 2017 (04).jpg」
  Author: 雷太
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Chichibu%27s_shibazakura,_Saitama_Prefecture;_April_2017_(04).jpg
  ※アプリ表示用にリサイズ (resized)

● 道後温泉
  「Dogo Onsen Honkan 道後溫泉本館 - panoramio (1).jpg」
  Author: lienyuan lee
  License: CC BY 3.0 (https://creativecommons.org/licenses/by/3.0)
  Source: https://commons.wikimedia.org/wiki/File:Dogo_Onsen_Honkan_%E9%81%93%E5%BE%8C%E6%BA%AB%E6%B3%89%E6%9C%AC%E9%A4%A8_-_panoramio_(1).jpg
  ※アプリ表示用にリサイズ (resized)

● 道頓堀
  「Dōtonbori at night, Osaka (19551420339).jpg」
  Author: Kristoffer Trolle from Copenhagen, Denmark
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:D%C5%8Dtonbori_at_night,_Osaka_(19551420339).jpg
  ※アプリ表示用にリサイズ (resized)

● 袋田の滝
  「Fukuroda Falls, Ibaraki (52531432264).jpg」
  Author: Raita Futo from Tokyo, Japan
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Fukuroda_Falls,_Ibaraki_(52531432264).jpg
  ※アプリ表示用にリサイズ (resized)

● 富良野ラベンダー畑
  「Sightseeing spot in Furano, Hokkaido Prefecture; July 2017 (02).jpg」
  Author: 雷太
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Sightseeing_spot_in_Furano,_Hokkaido_Prefecture;_July_2017_(02).jpg
  ※アプリ表示用にリサイズ (resized)

● 軍艦島
  「Hashima, Nagasaki, Japan, 20240814 1421 3377.jpg」
  Author: Jakub Hałun
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Hashima,_Nagasaki,_Japan,_20240814_1421_3377.jpg
  ※アプリ表示用にリサイズ (resized)

● 函館山
  「Hokkaido Hakodate night view xl.jpg」
  Author: 洞内定晴
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Hokkaido_Hakodate_night_view_xl.jpg
  ※アプリ表示用にリサイズ (resized)

● 飛騨高山
  「Hida Takayama old town streets (48519369602).jpg」
  Author: Raita Futo from Tokyo, Japan
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Hida_Takayama_old_town_streets_(48519369602).jpg
  ※アプリ表示用にリサイズ (resized)

● ひがし茶屋街
  「ひがし茶屋街 - panoramio (1).jpg」
  Author: くろふね
  License: CC BY 3.0 (https://creativecommons.org/licenses/by/3.0)
  Source: https://commons.wikimedia.org/wiki/File:%E3%81%B2%E3%81%8C%E3%81%97%E8%8C%B6%E5%B1%8B%E8%A1%97_-_panoramio_(1).jpg
  ※アプリ表示用にリサイズ (resized)

● 姫路城
  「Himeji Castle also known as the White Heron castle ("Shirasagijo") (53621201074).jpg」
  Author: Winniepix
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Himeji_Castle_also_known_as_the_White_Heron_castle_(%22Shirasagijo%22)_(53621201074).jpg
  ※アプリ表示用にリサイズ (resized)

● 国営ひたち海浜公園
  「Miharashino Oka (Hitachi Seaside Park) 45.jpg」
  Author: Σ64
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Miharashino_Oka_(Hitachi_Seaside_Park)_45.jpg
  ※アプリ表示用にリサイズ (resized)

● 犬山城
  「Inuyama Castle (Inuyama Aichi) VLux5 hdsr 08.jpg」
  Author: Hyppolyte de Saint-Rambert
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Inuyama_Castle_(Inuyama_Aichi)_VLux5_hdsr_08.jpg
  ※アプリ表示用にリサイズ (resized)

● 厳島神社
  「Itsukushima-jinja torii, Miyajima, Japan, 20240816 1716 4048.jpg」
  Author: Jakub Hałun
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Itsukushima-jinja_torii,_Miyajima,_Japan,_20240816_1716_4048.jpg
  ※アプリ表示用にリサイズ (resized)

● 祖谷のかずら橋
  「The Iya vine bridge 01157.jpg」
  Author: Mike B in Colorado
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:The_Iya_vine_bridge_01157.jpg
  ※アプリ表示用にリサイズ (resized)

● 出雲大社
  「Izumo Taisha Grand Shrine (51821905179).jpg」
  Author: Raita Futo from Tokyo, Japan
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Izumo_Taisha_Grand_Shrine_(51821905179).jpg
  ※アプリ表示用にリサイズ (resized)

● 地獄めぐり
  「The Beppu's Sea Hell (52133778575).jpg」
  Author: Raita Futo
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:The_Beppu%27s_Sea_Hell_(52133778575).jpg
  ※アプリ表示用にリサイズ (resized)

● 浄土ヶ浜
  「Jodogahama Beach (51971544590).jpg」
  Author: Raita Futo from Tokyo, Japan
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Jodogahama_Beach_(51971544590).jpg
  ※アプリ表示用にリサイズ (resized)

● 角館武家屋敷
  「Kakunodate 488.jpg」
  Author: Kmhkmh
  License: CC BY 3.0 (https://creativecommons.org/licenses/by/3.0)
  Source: https://commons.wikimedia.org/wiki/File:Kakunodate_488.jpg
  ※アプリ表示用にリサイズ (resized)

● 鎌倉大仏
  「The Daibutsu or Great Buddha of Kamakura (9412296776).jpg」
  Author: Maarten Heerlien from Voorschoten, The Netherlands
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:The_Daibutsu_or_Great_Buddha_of_Kamakura_(9412296776).jpg
  ※アプリ表示用にリサイズ (resized)

● 唐津城
  「Karatsu Castle (38299875212).jpg」
  Author: SuperTaiyaki from Osaka, Japan
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Karatsu_Castle_(38299875212).jpg
  ※アプリ表示用にリサイズ (resized)

● 春日大社
  「Nara Kasuga-taisha Main Sanctuary Cloister Lanterns 1.jpg」
  Author: Zairon
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Nara_Kasuga-taisha_Main_Sanctuary_Cloister_Lanterns_1.jpg
  ※アプリ表示用にリサイズ (resized)

● 川越
  「Japan Kawagoe.jpg」
  Author: Balabd
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Japan_Kawagoe.jpg
  ※アプリ表示用にリサイズ (resized)

● 華厳の滝
  「Kegon Falls (51988140443).jpg」
  Author: Raita Futo from Tokyo, Japan
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Kegon_Falls_(51988140443).jpg
  ※アプリ表示用にリサイズ (resized)

● 錦帯橋
  「Iwakuni, ponte kintai-kyo, 05.jpg」
  Author: Sailko
  License: CC BY 3.0 (https://creativecommons.org/licenses/by/3.0)
  Source: https://commons.wikimedia.org/wiki/File:Iwakuni,_ponte_kintai-kyo,_05.jpg
  ※アプリ表示用にリサイズ (resized)

● 後楽園
  「後楽園と城 by takeokahp - panoramio.jpg」
  Author: takeokahp
  License: CC BY 3.0 (https://creativecommons.org/licenses/by/3.0)
  Source: https://commons.wikimedia.org/wiki/File:%E5%BE%8C%E6%A5%BD%E5%9C%92%E3%81%A8%E5%9F%8E_by_takeokahp_-_panoramio.jpg
  ※アプリ表示用にリサイズ (resized)

● 熊本城
  「Kumamoto castle (Chuo-ku, Kumamoto) VLux5 hdsr kt03.jpg」
  Author: Hyppolyte de Saint-Rambert
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Kumamoto_castle_(Chuo-ku,_Kumamoto)_VLux5_hdsr_kt03.jpg
  ※アプリ表示用にリサイズ (resized)

● 倉敷美観地区
  「Kurashiki Canal 倉敷川 - panoramio.jpg」
  Author: lienyuan lee
  License: CC BY 3.0 (https://creativecommons.org/licenses/by/3.0)
  Source: https://commons.wikimedia.org/wiki/File:Kurashiki_Canal_%E5%80%89%E6%95%B7%E5%B7%9D_-_panoramio.jpg
  ※アプリ表示用にリサイズ (resized)

● 黒部ダム
  「Kurobe Dam 2015-09-23 01.jpg」
  Author: Kentaro Ohno
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Kurobe_Dam_2015-09-23_01.jpg
  ※アプリ表示用にリサイズ (resized)

● 琵琶湖
  「Shirahige Shrine 白鬚神社4 - panoramio.jpg」
  Author: baggio4ever
  License: CC BY 3.0 (https://creativecommons.org/licenses/by/3.0)
  Source: https://commons.wikimedia.org/wiki/File:Shirahige_Shrine_%E7%99%BD%E9%AC%9A%E7%A5%9E%E7%A4%BE4_-_panoramio.jpg
  ※アプリ表示用にリサイズ (resized)

● 万座毛
  「万座毛 20130204 - panoramio.jpg」
  Author: 690 Noda
  License: CC BY 3.0 (https://creativecommons.org/licenses/by/3.0)
  Source: https://commons.wikimedia.org/wiki/File:%E4%B8%87%E5%BA%A7%E6%AF%9B_20130204_-_panoramio.jpg
  ※アプリ表示用にリサイズ (resized)

● 松江城
  「080720 Matsue Castle Matsue Shimane pref Japan01s.jpg」
  Author: 663highland
  License: CC BY 2.5 (https://creativecommons.org/licenses/by/2.5)
  Source: https://commons.wikimedia.org/wiki/File:080720_Matsue_Castle_Matsue_Shimane_pref_Japan01s.jpg
  ※アプリ表示用にリサイズ (resized)

● 松島
  「Miyagi-Landscape of cherry blossoms and Matsushima Bay-xl.jpg」
  Author: Makoto_Lab
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Miyagi-Landscape_of_cherry_blossoms_and_Matsushima_Bay-xl.jpg
  ※アプリ表示用にリサイズ (resized)

● みなとみらい
  「Minato Mirai - Yokohama Skyline March 2025.jpg」
  Author: Justin Taylor
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Minato_Mirai_-_Yokohama_Skyline_March_2025.jpg
  ※アプリ表示用にリサイズ (resized)

● なばなの里
  「Nabana no Sato (Winter Illumination), Kuwana, Mie, Japan, なばなの里, 三重, 三重県 - panoramio (1).jpg」
  Author: carloshonda
  License: CC BY 3.0 (https://creativecommons.org/licenses/by/3.0)
  Source: https://commons.wikimedia.org/wiki/File:Nabana_no_Sato_(Winter_Illumination),_Kuwana,_Mie,_Japan,_%E3%81%AA%E3%81%B0%E3%81%AA%E3%81%AE%E9%87%8C,_%E4%B8%89%E9%87%8D,_%E4%B8%89%E9%87%8D%E7%9C%8C_-_panoramio_(1).jpg
  ※アプリ表示用にリサイズ (resized)

● 那智の滝
  「Kumano Nachi Taisha 02.jpg」
  Author: Douglas Perkins
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Kumano_Nachi_Taisha_02.jpg
  ※アプリ表示用にリサイズ (resized)

● 成田山新勝寺
  「Naritasan Shinshoji Temple.jpg」
  Author: Manish Prabhune
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Naritasan_Shinshoji_Temple.jpg
  ※アプリ表示用にリサイズ (resized)

● 登別地獄谷
  「Hell Valley (Jigokudani) - 22567574857.jpg」
  Author: Kentaro Ohno
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Hell_Valley_(Jigokudani)_-_22567574857.jpg
  ※アプリ表示用にリサイズ (resized)

● 大涌谷
  「Hakone 20221104 100950.jpg」
  Author: Ka23 13
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Hakone_20221104_100950.jpg
  ※アプリ表示用にリサイズ (resized)

● 桜島
  「Kagoshima view from Murasakibaru - Jan 29, 2012.jpg」
  Author: Karl Baron
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Kagoshima_view_from_Murasakibaru_-_Jan_29,_2012.jpg
  ※アプリ表示用にリサイズ (resized)

● 四天王寺
  「Shitennoji Osaka Tennoji-ku) hdsr S5 06.jpg」
  Author: Hyppolyte de Saint-Rambert
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Shitennoji_Osaka_Tennoji-ku)_hdsr_S5_06.jpg
  ※アプリ表示用にリサイズ (resized)

● 立山黒部アルペンルート
  「Mt Tateyama01s4592.jpg」
  Author: 663highland
  License: CC BY 2.5 (https://creativecommons.org/licenses/by/2.5)
  Source: https://commons.wikimedia.org/wiki/File:Mt_Tateyama01s4592.jpg
  ※アプリ表示用にリサイズ (resized)

● 東尋坊
  「Tojinbo cliffs, Fukui Prefecture; September 2019 (01).jpg」
  Author: 雷太
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:Tojinbo_cliffs,_Fukui_Prefecture;_September_2019_(01).jpg
  ※アプリ表示用にリサイズ (resized)

● 鳥取砂丘
  「Tottori Tottori Sand Dunes 04.jpg」
  Author: Zairon
  License: CC BY 4.0 (https://creativecommons.org/licenses/by/4.0)
  Source: https://commons.wikimedia.org/wiki/File:Tottori_Tottori_Sand_Dunes_04.jpg
  ※アプリ表示用にリサイズ (resized)

● 角島大橋
  「角島大橋 (38654569244).jpg」
  Author: Thomas Au
  License: CC BY 2.0 (https://creativecommons.org/licenses/by/2.0)
  Source: https://commons.wikimedia.org/wiki/File:%E8%A7%92%E5%B3%B6%E5%A4%A7%E6%A9%8B_(38654569244).jpg
  ※アプリ表示用にリサイズ (resized)

● 山寺
  「立石寺 Risshaku-ji Temple - panoramio (1).jpg」
  Author: lienyuan lee
  License: CC BY 3.0 (https://creativecommons.org/licenses/by/3.0)
  Source: https://commons.wikimedia.org/wiki/File:%E7%AB%8B%E7%9F%B3%E5%AF%BA_Risshaku-ji_Temple_-_panoramio_(1).jpg
  ※アプリ表示用にリサイズ (resized)

● 善光寺
  「160501 Zenkoji Nagano Japan01s3.jpg」
  Author: 663highland
  License: CC BY 2.5 (https://creativecommons.org/licenses/by/2.5)
  Source: https://commons.wikimedia.org/wiki/File:160501_Zenkoji_Nagano_Japan01s3.jpg
  ※アプリ表示用にリサイズ (resized)

● 瑞鳳殿
  「Zuiho-den17s3872.jpg」
  Author: 663highland
  License: CC BY 2.5 (https://creativecommons.org/licenses/by/2.5)
  Source: https://commons.wikimedia.org/wiki/File:Zuiho-den17s3872.jpg
  ※アプリ表示用にリサイズ (resized)

──────────────
パブリックドメイン / CC0（帰属義務なし・参考出典）
──────────────
● 会津若松城 — Umako
  https://commons.wikimedia.org/wiki/File:Tsuruga_Castle_2007.jpg
● 嵐山 — Marco Almbauer
  https://commons.wikimedia.org/wiki/File:Sagano_Bamboo_forest.jpg
● 原爆ドーム — Balon Greyjoy
  https://commons.wikimedia.org/wiki/File:20190317_Atomic_Bomb_Dome-1.jpg
● 平等院 — No machine-readable author provided. Fg2 assumed (based on copyright claims).
  https://commons.wikimedia.org/wiki/File:Byodoin-PhoenixHall-M1247.jpg
● 美ら海水族館 — Sgt. Josh Cox
  https://commons.wikimedia.org/wiki/File:USMC-081220-M-0902C-004.jpg
● 恐竜博物館 — 先従隗始
  https://commons.wikimedia.org/wiki/File:Fukui_Prefectural_Dinosaur_Museum_20210504_01.jpg
● 伏見稲荷大社 — Balon Greyjoy
  https://commons.wikimedia.org/wiki/File:20181110_Fushimi_Inari_Torii_9.jpg
● 銀閣寺 — Yamato369
  https://commons.wikimedia.org/wiki/File:Ginkakuji_20181015e.jpg
● 銀山温泉 — lumoplank
  https://commons.wikimedia.org/wiki/File:Ginzan_Onsen_in_blue_hour.jpg
● グラバー園 — Fg2
  https://commons.wikimedia.org/wiki/File:Nagasaki-Glover-Garden-5340.jpg
● 弘前城 — Kojin
  https://commons.wikimedia.org/wiki/File:Hirosaki_Castle.JPG
● 広島城 — Evelyn-rose
  https://commons.wikimedia.org/wiki/File:Hiroshima-Castle-1.jpg
● 皇居 — Bernard Spragg. NZ from Christchurch, New Zealand
  https://commons.wikimedia.org/wiki/File:Nijubashi_Bridge._Tokyo._(42521677522).jpg
● 犬吠埼灯台 — Aspere
  https://commons.wikimedia.org/wiki/File:250821_Inubosaki_Lighthouse_03.jpg
● 伊勢神宮 — MaedaAkihiko
  https://commons.wikimedia.org/wiki/File:Ise-Shrine_Naiku-Ujibashi-Torii.jpg
● 石垣島 — Metatron
  https://commons.wikimedia.org/wiki/File:Kabirawan.jpg
● 地獄谷野猿公苑 — nomao saeki saekinomao
  https://commons.wikimedia.org/wiki/File:Wild_Winter_Primates_(Unsplash).jpg
● 上高地 — pcs34560 from jawp

 This photo was taken with Canon EOS 500D‎
  https://commons.wikimedia.org/wiki/File:Kappabashi_bridge_Kamik%C5%8Dchi_20110717_0743_photo_by_Pcs34560.jpg
● 金沢城 — Balon Greyjoy
  https://commons.wikimedia.org/wiki/File:20190705_Kanazawa_Castle-8.jpg
● 桂浜 — baggio4ever
  https://commons.wikimedia.org/wiki/File:%E6%A1%82%E6%B5%9C_%E5%9D%82%E6%9C%AC%E9%BE%8D%E9%A6%AC%E5%83%8F1_Katsura-hama_Beach_-_panoramio.jpg
● 兼六園 — DimiTalen
  https://commons.wikimedia.org/wiki/File:View_towards_Hanami-bashi_with_sakura,_Kenroku-en,_Kanazawa,_2016.jpg
● 金閣寺 — Mustang Joe
  https://commons.wikimedia.org/wiki/File:Temple_of_the_Golden_Pavilion,_Kyoto_Japan_(14437492520).jpg
● 清津峡 — Saigen Jiro
  https://commons.wikimedia.org/wiki/File:Kiyotsukyo,_gaikan.jpg
● 神戸ポートタワー — Balon Greyjoy
  https://commons.wikimedia.org/wiki/File:20190901_Kobe_harbor-2.jpg
● 高知城 — Saigen Jiro
  https://commons.wikimedia.org/wiki/File:Kochi_Castle,_enkei.jpg
● 草津温泉 — Aspere
  https://commons.wikimedia.org/wiki/File:251128_Yubatake,_Kusatsu_04.jpg
● 河口湖 — Marion & Christoph Aistleitner
  https://commons.wikimedia.org/wiki/File:Fuji_Kawaguchi_452.JPG
● 田沢湖 — Brian Dell
  https://commons.wikimedia.org/wiki/File:Lake_Tazawa_in_winter.JPG
● 十和田湖 — Soica2001 (talk)
  https://commons.wikimedia.org/wiki/File:Lake_Towada_from_Ohanabe_2008.jpg
● 洞爺湖 — Kentaro Ohno
  https://commons.wikimedia.org/wiki/File:Naka-jima_island,_Lake_Toya,_Hokkaio_-_Jun_21,_2015.jpg
● 松本城 — Syced
  https://commons.wikimedia.org/wiki/File:Matsumoto_castle,_stitched_(69119).jpg
● 松山城 — Urashimataro
  https://commons.wikimedia.org/wiki/File:Matsuyama_Castle_Main.jpg
● 明治神宮 — Mustang Joe
  https://commons.wikimedia.org/wiki/File:Torii_gate_for_the_Meiji_Shrine_(14628328081).jpg
● メタセコイア並木 — 先従隗始
  https://commons.wikimedia.org/wiki/File:A_row_of_Metasequoia_trees_Makino_20200919.jpg
● 門司港レトロ — Kirin7739
  https://commons.wikimedia.org/wiki/File:View_of_Mojiko_Station_from_Mojiko_Retro.jpg
● 阿蘇山 — Lawsonstu
  https://commons.wikimedia.org/wiki/File:Mount_Nakadake,_Aso-san.jpg
● 大山 — Shellparakeet
  https://commons.wikimedia.org/wiki/File:381_series_midori_yakumo_livery_%26_mt._daisen.jpg
● 富士山 — Dang Son
  https://commons.wikimedia.org/wiki/File:12-Chureito-pagoda-and-Mount-Fuji-Japan_(29677439878).jpg
● 名古屋城 — Syced
  https://commons.wikimedia.org/wiki/File:Nagoya_castle_(53895).jpg
● 奈良公園 — Balon Greyjoy
  https://commons.wikimedia.org/wiki/File:20190121_Nara_deer-4.jpg
● 鳴門の渦潮 — "EMPe"
  https://commons.wikimedia.org/wiki/File:The_whirling_current_in_Naruto.JPG
● 二条城 — ほっきー
  https://commons.wikimedia.org/wiki/File:%E4%BA%8C%E6%9D%A1%E5%9F%8E_%E5%94%90%E9%96%80_2025.jpg
● 日光東照宮 — Marco Almbauer
  https://commons.wikimedia.org/wiki/File:Nikk%C5%8D_T%C5%8Dsh%C5%8D-g%C5%AB_4.jpg
● お台場 — Pierre Blaché from Paris, France
  https://commons.wikimedia.org/wiki/File:Tokyo_and_the_Rainbow_Bridge_from_Odaiba_(48838291248).jpg
● 奥入瀬渓流 — Daderot
  https://commons.wikimedia.org/wiki/File:Oirase_Mountain_Stream_-_Towada,_Aomori_-_DSC00907.jpg
● 大室山 — Immanuelle
  https://commons.wikimedia.org/wiki/File:Crater_of_Mount_Omuro_(Izu_Peninsula)-14.jpg
● 大阪城 — Balon Greyjoy
  https://commons.wikimedia.org/wiki/File:20190121_Osaka_Castle-1.jpg
● 小樽運河 — Komatta
  https://commons.wikimedia.org/wiki/File:Otaru_Canal_Center.jpg
● 大内宿 — ウィキ太郎　(Wiki Taro)
  https://commons.wikimedia.org/wiki/File:Ouchi-juku_Fukushima_Prefecture_Japan_2013-04-29b.jpg
● 栗林公園 — Fg2
  https://commons.wikimedia.org/wiki/File:Ritsurin-Garden-M3566.jpg
● 龍泉洞 — --[[利用者:あおもりくま|あおもりくま]]（会話） 2020年11月21日 (土) 01:16 (UTC)
  https://commons.wikimedia.org/wiki/File:Ryusendo_Cave_Underground_Lake_Cap_20201115.jpg
● 浅草寺 — Akonnchiroll
  https://commons.wikimedia.org/wiki/File:Sensoji_2023.jpg
● 新世界 — Sakai Yayoi
  https://commons.wikimedia.org/wiki/File:Shinsekai_and_Tsutenkaku_Tower.jpg
● 白浜温泉 — 玄史生
  https://commons.wikimedia.org/wiki/File:Engetsu_Island_Nearview.jpg
● 白糸の滝 — Saigen Jiro
  https://commons.wikimedia.org/wiki/File:Shiraito_Falls_(Shizuoka),_zenkei-2.jpg
● 白川郷 — Wicki
  https://commons.wikimedia.org/wiki/File:Traditional_Japanese_village_Shirakawa-go.JPG
● 首里城 — Fg2
  https://commons.wikimedia.org/wiki/File:Shuri_Castle_Scan_Slide.jpg
● 住吉大社 — 北谷2
  https://commons.wikimedia.org/wiki/File:Taikobashi_of_Sumiyoshitaisha.jpg
● 高千穂峡 — Max Smith
  https://commons.wikimedia.org/wiki/File:Takachiho_Gorge_by_boat.jpg
● 竹田城跡 — Brakeet
  https://commons.wikimedia.org/wiki/File:Takeda_Castle_2011_03.jpg
● 東大寺 — Balon Greyjoy
  https://commons.wikimedia.org/wiki/File:20190121_T%C5%8Ddai-ji_Great_Buddha_Hall-1.jpg
● 東京スカイツリー — Akonnchiroll
  https://commons.wikimedia.org/wiki/File:Tokyo_Skytree_2023.jpg
● 富岡製糸場 — C1815
  https://commons.wikimedia.org/wiki/File:Tomioka_Silk_Mill_Main_Building.JPG
● 鶴岡八幡宮 — Ocdp
  https://commons.wikimedia.org/wiki/File:Tsurugaoka_Hachimangu_001.jpg
● 弥彦神社 — Saigen Jiro
  https://commons.wikimedia.org/wiki/File:Iyahiko-jinja_haiden.JPG
● 屋久島 — MaedaAkihiko
  https://commons.wikimedia.org/wiki/File:Yaku-Island_Shiratani-Unsui-Gorge.jpg
● 柳川下り — No machine-readable author provided. Shii assumed (based on copyright claims).
  https://commons.wikimedia.org/wiki/File:Bridge_in_Yanagawa.jpg
● 屋島 — ラハール
  https://commons.wikimedia.org/wiki/File:Mt._Yashima.jpg
● 横浜中華街 — Syced
  https://commons.wikimedia.org/wiki/File:Yokohama_Chinatown_34.jpg
● 吉野ヶ里遺跡 — Saigen Jiro
  https://commons.wikimedia.org/wiki/File:Yoshinogari-iseki_tateanashiki-juukyo.JPG
● 湯布院 — Bobo12345 at English Wikipedia
  https://commons.wikimedia.org/wiki/File:Yufuin,_%C5%8Cita_Prefecture_(4_November_2006).jpg
"""
