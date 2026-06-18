package com.railway.interlocking.controller;

import com.railway.interlocking.dto.request.*;
import com.railway.interlocking.dto.response.ApiResponse;
import com.railway.interlocking.dto.response.InterlockingStatusResponse;
import com.railway.interlocking.dto.response.RouteOperationResponse;
import com.railway.interlocking.model.*;
import com.railway.interlocking.service.InterlockingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 联锁系统控制器
 * Interlocking Controller
 * 提供铁路联锁系统的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/interlocking")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterlockingController {

    private final InterlockingService interlockingService;

    /**
     * 获取联锁系统完整状态
     */
    @GetMapping("/status")
    public ApiResponse<InterlockingStatusResponse> getStatus() {
        log.info("获取联锁系统状态");
        InterlockingStatusResponse status = interlockingService.getInterlockingStatus();
        return ApiResponse.success("获取状态成功", status);
    }

    /**
     * 办理进路
     */
    @PostMapping("/route/establish")
    public ApiResponse<RouteOperationResponse> establishRoute(
            @Validated @RequestBody RouteEstablishRequest request) {
        log.info("办理进路请求: routeId={}, operator={}", request.getRouteId(), request.getOperator());
        RouteOperationResponse response = interlockingService.establishRoute(request);
        if (response.isSuccess()) {
            return ApiResponse.success(response.getMessage(), response);
        } else {
            return ApiResponse.error(400, response.getMessage());
        }
    }

    /**
     * 取消进路
     */
    @PostMapping("/route/cancel")
    public ApiResponse<RouteOperationResponse> cancelRoute(
            @Validated @RequestBody RouteCancelRequest request) {
        log.info("取消进路请求: routeId={}, operator={}", request.getRouteId(), request.getOperator());
        RouteOperationResponse response = interlockingService.cancelRoute(request);
        if (response.isSuccess()) {
            return ApiResponse.success(response.getMessage(), response);
        } else {
            return ApiResponse.error(400, response.getMessage());
        }
    }

    /**
     * 操作道岔
     */
    @PostMapping("/switch/operate")
    public ApiResponse<RouteOperationResponse> operateSwitch(
            @Validated @RequestBody SwitchOperateRequest request) {
        log.info("操作道岔请求: switchId={}, targetPosition={}, operator={}",
                request.getSwitchId(), request.getTargetPosition(), request.getOperator());
        RouteOperationResponse response = interlockingService.operateSwitch(request);
        if (response.isSuccess()) {
            return ApiResponse.success(response.getMessage(), response);
        } else {
            return ApiResponse.error(400, response.getMessage());
        }
    }

    /**
     * 控制信号机
     */
    @PostMapping("/signal/control")
    public ApiResponse<RouteOperationResponse> controlSignal(
            @Validated @RequestBody SignalControlRequest request) {
        log.info("控制信号机请求: signalId={}, targetAspect={}, operator={}",
                request.getSignalId(), request.getTargetAspect(), request.getOperator());
        RouteOperationResponse response = interlockingService.controlSignal(request);
        if (response.isSuccess()) {
            return ApiResponse.success(response.getMessage(), response);
        } else {
            return ApiResponse.error(400, response.getMessage());
        }
    }

    /**
     * 占用轨道区段（模拟列车）
     */
    @PostMapping("/section/occupy")
    public ApiResponse<RouteOperationResponse> occupySection(
            @Validated @RequestBody SectionOccupyRequest request) {
        log.info("占用区段请求: sectionId={}, trainId={}", request.getSectionId(), request.getTrainId());
        RouteOperationResponse response = interlockingService.occupySection(request);
        if (response.isSuccess()) {
            return ApiResponse.success(response.getMessage(), response);
        } else {
            return ApiResponse.error(400, response.getMessage());
        }
    }

    /**
     * 释放轨道区段（模拟列车出清）
     */
    @PostMapping("/section/release/{sectionId}")
    public ApiResponse<RouteOperationResponse> releaseSection(
            @PathVariable String sectionId,
            @RequestParam(required = false) String operator) {
        log.info("释放区段请求: sectionId={}, operator={}", sectionId, operator);
        RouteOperationResponse response = interlockingService.releaseSection(sectionId, operator);
        if (response.isSuccess()) {
            return ApiResponse.success(response.getMessage(), response);
        } else {
            return ApiResponse.error(400, response.getMessage());
        }
    }

    /**
     * 获取所有轨道区段
     */
    @GetMapping("/sections")
    public ApiResponse<List<TrackSection>> getAllSections() {
        log.info("获取所有轨道区段");
        List<TrackSection> sections = interlockingService.getAllTrackSections();
        return ApiResponse.success("获取成功", sections);
    }

    /**
     * 获取单个轨道区段
     */
    @GetMapping("/sections/{id}")
    public ApiResponse<TrackSection> getSectionById(@PathVariable String id) {
        log.info("获取轨道区段: id={}", id);
        TrackSection section = interlockingService.getTrackSection(id);
        if (section != null) {
            return ApiResponse.success("获取成功", section);
        } else {
            return ApiResponse.error(404, "轨道区段不存在");
        }
    }

    /**
     * 获取所有道岔
     */
    @GetMapping("/switches")
    public ApiResponse<List<Switch>> getAllSwitches() {
        log.info("获取所有道岔");
        List<Switch> switches = interlockingService.getAllSwitches();
        return ApiResponse.success("获取成功", switches);
    }

    /**
     * 获取单个道岔
     */
    @GetMapping("/switches/{id}")
    public ApiResponse<Switch> getSwitchById(@PathVariable String id) {
        log.info("获取道岔: id={}", id);
        Switch sw = interlockingService.getSwitch(id);
        if (sw != null) {
            return ApiResponse.success("获取成功", sw);
        } else {
            return ApiResponse.error(404, "道岔不存在");
        }
    }

    /**
     * 获取所有信号机
     */
    @GetMapping("/signals")
    public ApiResponse<List<Signal>> getAllSignals() {
        log.info("获取所有信号机");
        List<Signal> signals = interlockingService.getAllSignals();
        return ApiResponse.success("获取成功", signals);
    }

    /**
     * 获取单个信号机
     */
    @GetMapping("/signals/{id}")
    public ApiResponse<Signal> getSignalById(@PathVariable String id) {
        log.info("获取信号机: id={}", id);
        Signal signal = interlockingService.getSignal(id);
        if (signal != null) {
            return ApiResponse.success("获取成功", signal);
        } else {
            return ApiResponse.error(404, "信号机不存在");
        }
    }

    /**
     * 获取所有进路
     */
    @GetMapping("/routes")
    public ApiResponse<List<Route>> getAllRoutes() {
        log.info("获取所有进路");
        List<Route> routes = interlockingService.getAllRoutes();
        return ApiResponse.success("获取成功", routes);
    }

    /**
     * 获取单个进路
     */
    @GetMapping("/routes/{id}")
    public ApiResponse<Route> getRouteById(@PathVariable String id) {
        log.info("获取进路: id={}", id);
        Route route = interlockingService.getRoute(id);
        if (route != null) {
            return ApiResponse.success("获取成功", route);
        } else {
            return ApiResponse.error(404, "进路不存在");
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("铁路联锁系统运行正常", "OK");
    }
}
