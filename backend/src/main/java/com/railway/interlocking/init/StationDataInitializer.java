package com.railway.interlocking.init;

import com.railway.interlocking.model.*;
import com.railway.interlocking.model.enums.*;
import com.railway.interlocking.service.InterlockingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 站场数据初始化器
 * Station Data Initializer
 * 初始化标准高铁站场数据，包含：
 * - 100+ 轨道区段
 * - 30+ 道岔
 * - 40+ 信号机
 * - 50+ 进路
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationDataInitializer implements CommandLineRunner {

    private final InterlockingService interlockingService;

    private static final String STATION_ID = "STATION_001";
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化高铁站场数据...");

        List<TrackSection> sections = createTrackSections();
        List<Switch> switches = createSwitches();
        List<Signal> signals = createSignals();
        List<Route> routes = createRoutes(sections, switches, signals);

        setupRouteConflicts(routes);

        interlockingService.initializeStationData(sections, switches, signals, routes);

        log.info("高铁站场数据初始化完成!");
        log.info("  轨道区段: {} 个", sections.size());
        log.info("  道岔: {} 个", switches.size());
        log.info("  信号机: {} 个", signals.size());
        log.info("  进路: {} 个", routes.size());
    }

    /**
     * 创建轨道区段数据（108个）
     * 包括：正线、到发线、调车线、渡线、联络线等
     */
    private List<TrackSection> createTrackSections() {
        List<TrackSection> sections = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {
            sections.add(createSection("I" + i + "G", "Ⅰ道正线" + i + "区段", "正线", 800 + i * 50, 100 + i, 10, true));
        }

        for (int i = 1; i <= 4; i++) {
            sections.add(createSection("II" + i + "G", "Ⅱ道正线" + i + "区段", "正线", 800 + i * 50, 200 + i, 10, true));
        }

        for (int track = 1; track <= 12; track++) {
            for (int seg = 1; seg <= 5; seg++) {
                String id = String.format("%02dG%02d", track, seg);
                String name = String.format("%d道第%d区段", track, seg);
                double posX = 300 + track * 30;
                double posY = 50 + seg * 80;
                sections.add(createSection(id, name, "到发线", 200 + seg * 30, posX, posY, true));
            }
        }

        String[] shuntingNames = {
            "调车1区", "调车2区", "调车3区", "调车4区", "调车5区",
            "调车6区", "调车7区", "调车8区", "调车9区", "调车10区"
        };
        for (int i = 0; i < shuntingNames.length; i++) {
            String id = "DC" + (i + 1) + "G";
            sections.add(createSection(id, shuntingNames[i], "调车线", 150 + i * 20, 50 + i * 15, 350, false));
        }

        for (int i = 1; i <= 12; i++) {
            String id = "DX" + i + "G";
            String name = i + "号渡线";
            sections.add(createSection(id, name, "渡线", 50 + i * 5, 100 + i * 25, 80 + i * 20, true));
        }

        sections.add(createSection("LL1G", "联络线1区段", "联络线", 500, 50, 200, true));
        sections.add(createSection("LL2G", "联络线2区段", "联络线", 450, 50, 220, true));
        sections.add(createSection("LL3G", "联络线3区段", "联络线", 480, 50, 240, true));

        sections.add(createSection("CJ1G", "出站1区段", "出站线", 300, 100, 0, true));
        sections.add(createSection("CJ2G", "出站2区段", "出站线", 300, 200, 0, true));
        sections.add(createSection("RJ1G", "入站1区段", "入站线", 300, 100, 500, true));
        sections.add(createSection("RJ2G", "入站2区段", "入站线", 300, 200, 500, true));

        sections.add(createSection("JG1G", "进站预告1区段", "预告区段", 600, 100, 600, true));
        sections.add(createSection("JG2G", "进站预告2区段", "预告区段", 600, 200, 600, true));
        sections.add(createSection("CG1G", "出站轨道1区段", "出站区段", 400, 100, -50, true));
        sections.add(createSection("CG2G", "出站轨道2区段", "出站区段", 400, 200, -50, true));

        sections.add(createSection("ZD1G", "折返1区段", "折返线", 250, 50, 450, true));
        sections.add(createSection("ZD2G", "折返2区段", "折返线", 250, 250, 450, true));
        sections.add(createSection("JX1G", "机走线1区段", "机走线", 180, 150, 400, false));
        sections.add(createSection("JX2G", "机走线2区段", "机走线", 180, 150, 420, false));

        for (int i = 1; i <= 4; i++) {
            String id = "BL" + i + "G";
            String name = "闭塞分区" + i;
            sections.add(createSection(id, name, "闭塞分区", 1500, 100 + i * 20, -200 - i * 50, true));
        }

        sections.add(createSection("JGZG", "进站正线轨道区段", "正线", 200, 150, 550, true));
        sections.add(createSection("CZDG", "出站轨道电路区段", "出站", 150, 150, -100, true));
        sections.add(createSection("FWG", "防护区段", "防护", 100, 150, 650, true));
        sections.add(createSection("JCG", "检测区段", "检测", 80, 0, 0, true));

        log.info("创建轨道区段: {} 个", sections.size());
        return sections;
    }

    /**
     * 创建单个轨道区段
     */
    private TrackSection createSection(String id, String name, String type, double length,
                                       double posX, double posY, boolean electrified) {
        return TrackSection.builder()
                .id(id)
                .name(name)
                .description(name + " - " + type)
                .stationId(STATION_ID)
                .length(length)
                .sectionType(type)
                .status(TrackSectionStatus.IDLE)
                .occupiedByTrainId(null)
                .lockedByRouteId(null)
                .speedLimit(type.contains("正线") ? 350 : (type.contains("到发") ? 120 : 80))
                .positionX(posX)
                .positionY(posY)
                .electrified(electrified)
                .lastUpdateTime(NOW)
                .statusRemark("初始化")
                .build();
    }

    /**
     * 创建道岔数据（32个）
     */
    private List<Switch> createSwitches() {
        List<Switch> switches = new ArrayList<>();

        for (int i = 1; i <= 24; i++) {
            String toeSection = i <= 12 ? "I1G" : "II1G";
            String normalSection = String.format("%02dG01", i <= 12 ? i : i - 12);
            String reverseSection = String.format("%02dG01", i <= 12 ? i + 1 : i - 11);

            switches.add(createSwitch(
                    String.valueOf(i),
                    i + "号道岔",
                    "单开道岔",
                    18,
                    toeSection,
                    normalSection,
                    reverseSection,
                    100 + i * 20,
                    100 + i * 15
            ));
        }

        for (int i = 25; i <= 30; i++) {
            int idx = i - 24;
            switches.add(createSwitch(
                    String.valueOf(i),
                    i + "号渡线道岔",
                    "渡线道岔",
                    12,
                    "DX" + idx + "G",
                    String.format("%02dG03", idx),
                    String.format("%02dG03", idx + 1),
                    100 + idx * 20,
                    250 + idx * 15
            ));
        }

        switches.add(createSwitch("31", "31号联络线道岔", "单开道岔", 18,
                "LL1G", "I4G", "LL2G", 80, 180));
        switches.add(createSwitch("32", "32号联络线道岔", "单开道岔", 18,
                "LL3G", "II4G", "LL2G", 80, 220));

        log.info("创建道岔: {} 个", switches.size());
        return switches;
    }

    /**
     * 创建单个道岔
     */
    private Switch createSwitch(String number, String name, String type, int frogNumber,
                                String toeSection, String normalSection, String reverseSection,
                                double posX, double posY) {
        return Switch.builder()
                .id("SW" + number)
                .number(number)
                .name(name)
                .description(name + " - " + type)
                .stationId(STATION_ID)
                .switchType(type)
                .frogNumber(frogNumber)
                .position(SwitchPosition.NORMAL)
                .targetPosition(SwitchPosition.NORMAL)
                .indication(SwitchPosition.NORMAL)
                .locked(false)
                .lockedByRouteId(null)
                .normalSectionId(normalSection)
                .reverseSectionId(reverseSection)
                .toeSectionId(toeSection)
                .normalSpeedLimit(frogNumber >= 18 ? 160 : 80)
                .reverseSpeedLimit(frogNumber >= 18 ? 80 : 45)
                .positionX(posX)
                .positionY(posY)
                .operationTime(3)
                .lastOperationTime(NOW)
                .lastUpdateTime(NOW)
                .electrified(true)
                .statusRemark("初始化")
                .build();
    }

    /**
     * 创建信号机数据（42个）
     */
    private List<Signal> createSignals() {
        List<Signal> signals = new ArrayList<>();

        signals.add(createSignal("S", "S进站信号机", "进站", "上行",
                Arrays.asList("JG1G", "JGZG", "I1G"), "JGZG",
                Arrays.asList("RED", "YELLOW", "GREEN", "DOUBLE_YELLOW"), 100, 580, true));
        signals.add(createSignal("X", "X进站信号机", "进站", "下行",
                Arrays.asList("JG2G", "JGZG", "II1G"), "JGZG",
                Arrays.asList("RED", "YELLOW", "GREEN", "DOUBLE_YELLOW"), 200, 580, true));

        for (int i = 1; i <= 12; i++) {
            String id = "S" + i;
            String name = i + "道上行出站信号机";
            String section = String.format("%02dG05", i);
            signals.add(createSignal(id, name, "出站", "上行",
                    Arrays.asList(section, "CG1G"), section,
                    Arrays.asList("RED", "YELLOW", "GREEN"),
                    300 + i * 30, 450, false));
        }

        for (int i = 1; i <= 12; i++) {
            String id = "X" + i;
            String name = i + "道下行出站信号机";
            String section = String.format("%02dG01", i);
            signals.add(createSignal(id, name, "出站", "下行",
                    Arrays.asList(section, "RJ1G"), section,
                    Arrays.asList("RED", "YELLOW", "GREEN"),
                    300 + i * 30, 50, false));
        }

        for (int i = 1; i <= 6; i++) {
            String id = "SL" + i;
            String name = i + "号上行进路信号机";
            String section = String.format("%02dG03", i * 2);
            signals.add(createSignal(id, name, "进路", "上行",
                    Arrays.asList(section), section,
                    Arrays.asList("RED", "YELLOW", "GREEN"),
                    300 + i * 60, 250, false));
        }

        for (int i = 1; i <= 6; i++) {
            String id = "XL" + i;
            String name = i + "号下行进路信号机";
            String section = String.format("%02dG03", i * 2);
            signals.add(createSignal(id, name, "进路", "下行",
                    Arrays.asList(section), section,
                    Arrays.asList("RED", "YELLOW", "GREEN"),
                    300 + i * 60, 250, false));
        }

        for (int i = 1; i <= 5; i++) {
            String id = "D" + i;
            String name = i + "号调车信号机";
            String section = "DC" + i + "G";
            signals.add(createSignal(id, name, "调车", "调车",
                    Arrays.asList(section), section,
                    Arrays.asList("RED", "YELLOW"),
                    50 + i * 15, 350, true));
        }

        signals.add(createSignal("SY", "上行预告信号机", "预告", "上行",
                Arrays.asList("JG1G"), "JG1G",
                Arrays.asList("YELLOW", "GREEN"), 100, 650, false));
        signals.add(createSignal("XY", "下行预告信号机", "预告", "下行",
                Arrays.asList("JG2G"), "JG2G",
                Arrays.asList("YELLOW", "GREEN"), 200, 650, false));

        signals.add(createSignal("SZ", "上行通过信号机", "通过", "上行",
                Arrays.asList("BL1G", "BL2G"), "BL1G",
                Arrays.asList("RED", "YELLOW", "GREEN"), 100, -150, true));
        signals.add(createSignal("XZ", "下行通过信号机", "通过", "下行",
                Arrays.asList("BL3G", "BL4G"), "BL3G",
                Arrays.asList("RED", "YELLOW", "GREEN"), 200, -150, true));

        signals.add(createSignal("LLS", "联络线上行信号机", "出站", "上行",
                Arrays.asList("LL1G", "LL2G"), "LL1G",
                Arrays.asList("RED", "YELLOW", "GREEN"), 50, 200, true));
        signals.add(createSignal("LLX", "联络线下行信号机", "出站", "下行",
                Arrays.asList("LL3G", "LL2G"), "LL3G",
                Arrays.asList("RED", "YELLOW", "GREEN"), 50, 240, true));

        log.info("创建信号机: {} 个", signals.size());
        return signals;
    }

    /**
     * 创建单个信号机
     */
    private Signal createSignal(String id, String name, String type, String direction,
                                List<String> protectedSections, String locationSection,
                                List<String> lampConfig, double posX, double posY, boolean guidance) {
        return Signal.builder()
                .id(id)
                .number(id)
                .name(name)
                .description(name + " - " + type)
                .stationId(STATION_ID)
                .signalType(type)
                .direction(direction)
                .aspect(SignalAspect.RED)
                .targetAspect(SignalAspect.RED)
                .cleared(false)
                .locked(false)
                .guidanceAllowed(guidance)
                .clearedByRouteId(null)
                .protectedSectionIds(protectedSections)
                .locationSectionId(locationSection)
                .lampConfiguration(lampConfig)
                .maximumAspect(SignalAspect.GREEN)
                .positionX(posX)
                .positionY(posY)
                .displayDistance(1000)
                .lastUpdateTime(NOW)
                .lastClearTime(null)
                .electrified(true)
                .lampOk(true)
                .statusRemark("初始化")
                .build();
    }

    /**
     * 创建进路数据（56个）
     */
    private List<Route> createRoutes(List<TrackSection> sections,
                                      List<Switch> switches,
                                      List<Signal> signals) {
        List<Route> routes = new ArrayList<>();
        int routeNumber = 1;

        for (int track = 1; track <= 12; track++) {
            Route route = createReceptionRoute(routeNumber++, track, "上行", sections, signals);
            routes.add(route);
        }

        for (int track = 1; track <= 12; track++) {
            Route route = createReceptionRoute(routeNumber++, track, "下行", sections, signals);
            routes.add(route);
        }

        for (int track = 1; track <= 12; track++) {
            Route route = createDepartureRoute(routeNumber++, track, "上行", sections, signals);
            routes.add(route);
        }

        for (int track = 1; track <= 12; track++) {
            Route route = createDepartureRoute(routeNumber++, track, "下行", sections, signals);
            routes.add(route);
        }

        for (int track = 1; track <= 4; track++) {
            Route route = createThroughRoute(routeNumber++, track, "上行", sections, signals);
            routes.add(route);
        }

        for (int track = 1; track <= 4; track++) {
            Route route = createThroughRoute(routeNumber++, track, "下行", sections, signals);
            routes.add(route);
        }

        log.info("创建进路: {} 个", routes.size());
        return routes;
    }

    /**
     * 创建接车进路
     */
    private Route createReceptionRoute(int number, int track, String direction,
                                        List<TrackSection> sections, List<Signal> signals) {
        String routeId = String.format("ROUTE_R%03d", number);
        String startSignalId = direction.equals("上行") ? "S" : "X";
        String endSignalId = direction.equals("上行") ? "S" + track : "X" + track;

        List<String> sectionIds = new ArrayList<>();
        Map<String, SwitchPosition> switchPositions = new HashMap<>();
        List<String> switchIds = new ArrayList<>();
        List<String> signalIds = new ArrayList<>();
        List<String> hostileSignals = new ArrayList<>();

        sectionIds.add(direction.equals("上行") ? "JG1G" : "JG2G");
        sectionIds.add("JGZG");

        int startTrack = direction.equals("上行") ? 1 : 12;
        int endTrack = track;
        int step = direction.equals("上行") ? 1 : -1;

        for (int t = startTrack; t != endTrack + step; t += step) {
            for (int seg = 1; seg <= 5; seg++) {
                sectionIds.add(String.format("%02dG%02d", t, seg));
            }
            if (t != endTrack) {
                int swNum = direction.equals("上行") ? t : t - 1;
                String switchId = "SW" + swNum;
                switchIds.add(switchId);
                switchPositions.put(switchId,
                        t < endTrack ? SwitchPosition.REVERSE : SwitchPosition.NORMAL);
            }
        }

        signalIds.add(startSignalId);
        signalIds.add(endSignalId);

        for (int t = 1; t <= 12; t++) {
            if (t != track) {
                hostileSignals.add(direction.equals("上行") ? "S" + t : "X" + t);
            }
        }
        hostileSignals.add(direction.equals("上行") ? "X" : "S");

        double length = sectionIds.stream()
                .map(id -> sections.stream().filter(s -> s.getId().equals(id)).findFirst())
                .filter(Optional::isPresent)
                .mapToDouble(s -> s.get().getLength())
                .sum();

        return Route.builder()
                .id(routeId)
                .number(String.format("R%03d", number))
                .name(direction + "接" + track + "道")
                .description(direction + "方向接车进路至" + track + "道")
                .stationId(STATION_ID)
                .routeType(RouteType.RECEPTION)
                .direction(direction)
                .status(RouteStatus.NOT_ESTABLISHED)
                .startSignalId(startSignalId)
                .endSignalId(endSignalId)
                .sectionIds(sectionIds)
                .switchPositions(switchPositions)
                .switchIds(switchIds)
                .signalIds(signalIds)
                .conflictingRouteIds(new ArrayList<>())
                .hostileSignalIds(hostileSignals)
                .length(length)
                .speedLimit(80)
                .occupiedByTrainId(null)
                .operator(null)
                .establishedTime(null)
                .lockedTime(null)
                .clearedTime(null)
                .occupiedTime(null)
                .unlockedTime(null)
                .lastUpdateTime(NOW)
                .autoUnlock(true)
                .approachLocked(false)
                .approachLockRemainingTime(0)
                .guidanceRoute(false)
                .statusRemark("初始化")
                .build();
    }

    /**
     * 创建发车进路
     */
    private Route createDepartureRoute(int number, int track, String direction,
                                        List<TrackSection> sections, List<Signal> signals) {
        String routeId = String.format("ROUTE_D%03d", number);
        String startSignalId = direction.equals("上行") ? "S" + track : "X" + track;
        String endSignalId = direction.equals("上行") ? "SZ" : "XZ";

        List<String> sectionIds = new ArrayList<>();
        Map<String, SwitchPosition> switchPositions = new HashMap<>();
        List<String> switchIds = new ArrayList<>();
        List<String> signalIds = new ArrayList<>();
        List<String> hostileSignals = new ArrayList<>();

        for (int seg = 5; seg >= 1; seg--) {
            sectionIds.add(String.format("%02dG%02d", track, seg));
        }

        int startTrack = track;
        int endTrack = direction.equals("上行") ? 1 : 12;
        int step = direction.equals("上行") ? -1 : 1;

        for (int t = startTrack; t != endTrack + step; t += step) {
            if (t != endTrack) {
                int swNum = direction.equals("上行") ? t - 1 : t;
                String switchId = "SW" + swNum;
                switchIds.add(switchId);
                switchPositions.put(switchId,
                        t > endTrack ? SwitchPosition.REVERSE : SwitchPosition.NORMAL);
            }
        }

        sectionIds.add(direction.equals("上行") ? "CG1G" : "CG2G");
        sectionIds.add("CZDG");

        signalIds.add(startSignalId);
        signalIds.add(endSignalId);

        for (int t = 1; t <= 12; t++) {
            if (t != track) {
                hostileSignals.add(direction.equals("上行") ? "S" + t : "X" + t);
            }
        }

        double length = sectionIds.stream()
                .map(id -> sections.stream().filter(s -> s.getId().equals(id)).findFirst())
                .filter(Optional::isPresent)
                .mapToDouble(s -> s.get().getLength())
                .sum();

        return Route.builder()
                .id(routeId)
                .number(String.format("D%03d", number))
                .name(track + "道发" + (direction.equals("上行") ? "S" : "X"))
                .description(track + "道发车至" + direction + "方向")
                .stationId(STATION_ID)
                .routeType(RouteType.DEPARTURE)
                .direction(direction)
                .status(RouteStatus.NOT_ESTABLISHED)
                .startSignalId(startSignalId)
                .endSignalId(endSignalId)
                .sectionIds(sectionIds)
                .switchPositions(switchPositions)
                .switchIds(switchIds)
                .signalIds(signalIds)
                .conflictingRouteIds(new ArrayList<>())
                .hostileSignalIds(hostileSignals)
                .length(length)
                .speedLimit(120)
                .occupiedByTrainId(null)
                .operator(null)
                .establishedTime(null)
                .lockedTime(null)
                .clearedTime(null)
                .occupiedTime(null)
                .unlockedTime(null)
                .lastUpdateTime(NOW)
                .autoUnlock(true)
                .approachLocked(false)
                .approachLockRemainingTime(0)
                .guidanceRoute(false)
                .statusRemark("初始化")
                .build();
    }

    /**
     * 创建通过进路
     */
    private Route createThroughRoute(int number, int track, String direction,
                                      List<TrackSection> sections, List<Signal> signals) {
        String routeId = String.format("ROUTE_T%03d", number);
        String startSignalId = direction.equals("上行") ? "S" : "X";
        String endSignalId = direction.equals("上行") ? "SZ" : "XZ";

        List<String> sectionIds = new ArrayList<>();
        Map<String, SwitchPosition> switchPositions = new HashMap<>();
        List<String> switchIds = new ArrayList<>();
        List<String> signalIds = new ArrayList<>();
        List<String> hostileSignals = new ArrayList<>();

        sectionIds.add(direction.equals("上行") ? "JG1G" : "JG2G");
        sectionIds.add("JGZG");

        String prefix = direction.equals("上行") ? "I" : "II";
        for (int i = 1; i <= 4; i++) {
            sectionIds.add(prefix + i + "G");
        }

        sectionIds.add(direction.equals("上行") ? "CG1G" : "CG2G");
        sectionIds.add("CZDG");

        signalIds.add(startSignalId);
        signalIds.add(endSignalId);

        for (int t = 1; t <= 12; t++) {
            hostileSignals.add(direction.equals("上行") ? "S" + t : "X" + t);
            hostileSignals.add(direction.equals("上行") ? "X" + t : "S" + t);
        }

        double length = sectionIds.stream()
                .map(id -> sections.stream().filter(s -> s.getId().equals(id)).findFirst())
                .filter(Optional::isPresent)
                .mapToDouble(s -> s.get().getLength())
                .sum();

        return Route.builder()
                .id(routeId)
                .number(String.format("T%03d", number))
                .name(direction + "通过" + prefix + "道")
                .description(direction + "方向通过进路经" + prefix + "道正线")
                .stationId(STATION_ID)
                .routeType(RouteType.THROUGH)
                .direction(direction)
                .status(RouteStatus.NOT_ESTABLISHED)
                .startSignalId(startSignalId)
                .endSignalId(endSignalId)
                .sectionIds(sectionIds)
                .switchPositions(switchPositions)
                .switchIds(switchIds)
                .signalIds(signalIds)
                .conflictingRouteIds(new ArrayList<>())
                .hostileSignalIds(hostileSignals)
                .length(length)
                .speedLimit(250)
                .occupiedByTrainId(null)
                .operator(null)
                .establishedTime(null)
                .lockedTime(null)
                .clearedTime(null)
                .occupiedTime(null)
                .unlockedTime(null)
                .lastUpdateTime(NOW)
                .autoUnlock(true)
                .approachLocked(false)
                .approachLockRemainingTime(0)
                .guidanceRoute(false)
                .statusRemark("初始化")
                .build();
    }

    /**
     * 设置进路冲突关系
     */
    private void setupRouteConflicts(List<Route> routes) {
        Map<String, Route> routeMap = new HashMap<>();
        for (Route route : routes) {
            routeMap.put(route.getId(), route);
        }

        for (Route route : routes) {
            Set<String> conflicts = new HashSet<>();
            Set<String> routeSections = new HashSet<>(route.getSectionIds());
            Set<String> routeSwitches = new HashSet<>(route.getSwitchIds());

            for (Route other : routes) {
                if (route.getId().equals(other.getId())) {
                    continue;
                }

                boolean hasSectionConflict = other.getSectionIds().stream()
                        .anyMatch(routeSections::contains);

                boolean hasSwitchConflict = other.getSwitchIds().stream()
                        .anyMatch(routeSwitches::contains);

                boolean oppositeDirection = !route.getDirection().equals(other.getDirection())
                        && route.getRouteType() == other.getRouteType();

                if (hasSectionConflict || hasSwitchConflict || oppositeDirection) {
                    if (isConflictRoute(route, other)) {
                        conflicts.add(other.getId());
                    }
                }
            }

            route.setConflictingRouteIds(new ArrayList<>(conflicts));

            if (conflicts.size() > 0) {
                log.debug("进路[{}]冲突进路数: {}", route.getName(), conflicts.size());
            }
        }
    }

    /**
     * 判断两条进路是否为冲突进路
     */
    private boolean isConflictRoute(Route r1, Route r2) {
        if (r1.getRouteType() == RouteType.THROUGH
                && r2.getRouteType() == RouteType.THROUGH
                && r1.getDirection().equals(r2.getDirection())) {
            return false;
        }

        if (r1.getRouteType() == RouteType.RECEPTION
                && r2.getRouteType() == RouteType.DEPARTURE
                && r1.getDirection().equals(r2.getDirection())) {
            String r1Track = extractTrackNumber(r1.getName());
            String r2Track = extractTrackNumber(r2.getName());
            if (r1Track != null && r1Track.equals(r2Track)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 从进路名称中提取轨道号
     */
    private String extractTrackNumber(String name) {
        for (int i = 1; i <= 12; i++) {
            if (name.contains(i + "道")) {
                return String.valueOf(i);
            }
        }
        return null;
    }
}
