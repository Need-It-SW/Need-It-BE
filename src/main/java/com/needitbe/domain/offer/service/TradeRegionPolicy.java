package com.needitbe.domain.offer.service;

import java.util.Map;

/** Compare city/county for provinces and district/county for metropolitan cities. */
public final class TradeRegionPolicy {
    private TradeRegionPolicy() {}
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("서울", "서울특별시"), Map.entry("부산", "부산광역시"),
            Map.entry("대구", "대구광역시"), Map.entry("인천", "인천광역시"),
            Map.entry("광주", "광주광역시"), Map.entry("대전", "대전광역시"),
            Map.entry("울산", "울산광역시"), Map.entry("세종", "세종특별자치시"),
            Map.entry("경기", "경기도"), Map.entry("강원", "강원특별자치도"),
            Map.entry("충북", "충청북도"), Map.entry("충남", "충청남도"),
            Map.entry("전북", "전북특별자치도"), Map.entry("전남", "전라남도"),
            Map.entry("경북", "경상북도"), Map.entry("경남", "경상남도"),
            Map.entry("제주", "제주특별자치도")
    );
    public static boolean canOffer(String memberRegion, String postRegion) {
        String a = tradeArea(memberRegion);
        return a != null && a.equals(tradeArea(postRegion));
    }
    private static String tradeArea(String region) {
        if (region == null || region.isBlank()) return null;
        String[] parts = region.strip().split("\\s+");
        if (parts.length < 2) return null;
        String province = ALIASES.getOrDefault(parts[0], parts[0]);
        if (province.equals("세종특별자치시")) return province;
        boolean metro = province.endsWith("특별시") || province.endsWith("광역시");
        if (metro) {
            String district = parts[1];
            return (district.endsWith("구") || district.endsWith("군")) ? province + ":" + district : null;
        }
        // For e.g. 성남시 분당구 백현동 -> 성남시, but a county -> 군.
        String cityOrCounty = parts[1];
        return (cityOrCounty.endsWith("시") || cityOrCounty.endsWith("군"))
                ? province + ":" + cityOrCounty : null;
    }
}
