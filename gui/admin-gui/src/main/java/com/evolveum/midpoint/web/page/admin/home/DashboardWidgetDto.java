/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.home;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DashboardWidgetDto implements Serializable {

    public static final String F_NUMBER_LABEL = "numberLabel";
    public static final String F_MESSAGE = "message";
    public static final String F_STYLE_COLOR = "styleColor";
    public static final String F_STYLE_CSS_STYLE = "styleCssStyle";
    public static final String D_ICON_CSS_CLASS = "iconCssClass";

    private String numberLabel;
    private String message;
    private String styleColor;
    private String styleCssStyle;
    private String iconCssClass;

    private static final String NUMBER_MESSAGE_UNKNOWN = "InfoBoxPanel.message.unknown";
    private static final String DEFAULT_STATUS_COLOR = "#00a65a";
    private static final String NEUTRAL_STATUS_COLOR = "#6c757d";
    private static final String WARNING_STATUS_COLOR = "#f0ad4e";
    private static final String DEFAULT_ICON = "fa fa-question";
    private static final Pattern VALUE_OF_DOMAIN_PATTERN = Pattern.compile("^(\\d+)\\s*/\\s*(\\d+)\\b");

    public DashboardWidgetDto(DashboardWidget dashboardWidget, PageBase pageBase) {
        this.numberLabel = createNumberLabel(dashboardWidget, pageBase);
        this.message = createMessage(dashboardWidget);
        this.styleColor = createStyleColor(dashboardWidget);
        this.styleCssStyle = createStyleCssStyle(dashboardWidget);
        this.iconCssClass = createIconCssClass(dashboardWidget);
    }

    private String createNumberLabel(DashboardWidget widget, PageBase pageBase) {
        if (widget == null) {
            return pageBase.createStringResource(NUMBER_MESSAGE_UNKNOWN).getString();
        }

        String numberMessage = widget.getNumberMessage();
        if (numberMessage == null) {
            return pageBase.createStringResource(NUMBER_MESSAGE_UNKNOWN).getString();
        }

        return numberMessage; //number message have to add before icon because is needed evaluate variation
    }

    private String createMessage(DashboardWidget dashboardWidget) {
        if (dashboardWidget == null) {
            return null;
        }
        DisplayType displayType = dashboardWidget.getDisplay();
        if (displayType != null && displayType.getLabel() != null) {
            return WebComponentUtil.getTranslatedPolyString(displayType.getLabel());
        }

        DashboardWidgetType dashboardWidgetType = dashboardWidget.getWidget();
        if (dashboardWidgetType == null) {
            return null;
        }
        return dashboardWidgetType.getIdentifier();
    }

    private String createStyleColor(DashboardWidget dashboardWidget) {
        if (dashboardWidget == null) {
            return createStatusColorStyle(DEFAULT_STATUS_COLOR);
        }
        DisplayType displayType = dashboardWidget.getDisplay();
        if (displayType != null && StringUtils.isNoneBlank(displayType.getColor())) {
            String configuredColor = GuiDisplayTypeUtil.removeStringAfterSemicolon(displayType.getColor());
            return createStatusColorStyle(resolveAdminDashboardStatusColor(dashboardWidget, configuredColor));
        }
        return createStatusColorStyle(DEFAULT_STATUS_COLOR);
    }

    /**
     * The standard Admin dashboard represents two counts as "value/domain".
     * An empty resource set is informational, not a failure; and a partial
     * runnable-task set deserves attention unless its own danger variation is
     * already active. Keep all other configured dashboard colors untouched.
     */
    private String resolveAdminDashboardStatusColor(DashboardWidget dashboardWidget, String configuredColor) {
        DashboardWidgetType widget = dashboardWidget.getWidget();
        if (widget == null || StringUtils.isBlank(widget.getIdentifier())) {
            return configuredColor;
        }

        String numberMessage = dashboardWidget.getNumberMessage();
        if (StringUtils.isBlank(numberMessage)) {
            return configuredColor;
        }

        Matcher matcher = VALUE_OF_DOMAIN_PATTERN.matcher(numberMessage);
        if (!matcher.find()) {
            return configuredColor;
        }

        int value = Integer.parseInt(matcher.group(1));
        int domain = Integer.parseInt(matcher.group(2));
        String identifier = widget.getIdentifier();

        if ("resources-up".equals(identifier) && value == 0 && domain == 0) {
            return NEUTRAL_STATUS_COLOR;
        }

        if ("active-task".equals(identifier)
                && value < domain
                && StringUtils.equalsIgnoreCase(configuredColor, DEFAULT_STATUS_COLOR)) {
            return WARNING_STATUS_COLOR;
        }

        return configuredColor;
    }

    private String createStyleCssStyle(DashboardWidget dashboardWidget) {
        if (dashboardWidget == null) {
            return null;
        }
        DisplayType displayType = dashboardWidget.getDisplay();
        if (displayType != null && StringUtils.isNoneBlank(displayType.getCssStyle())) {
            // The theme controls widget text contrast. Preserve any other configured style,
            // but do not let an inline color override the status-aware card presentation.
            return displayType.getCssStyle().replaceAll("(?i)(^|;)\\s*color\\s*:[^;]*;?", "$1").trim();
        }
        return null;
    }

    private String createStatusColorStyle(String color) {
        return "--dashboard-widget-status-color:" + color + ";";
    }

    private String createIconCssClass(DashboardWidget dashboardWidget) {
        if (dashboardWidget == null) {
            return DEFAULT_ICON;
        }
        DisplayType displayType = dashboardWidget.getDisplay();
        if (displayType == null) {
            return DEFAULT_ICON;
        }

        IconType icon = displayType.getIcon();
        if (icon == null) {
            return DEFAULT_ICON;
        }

        String cssClass = icon.getCssClass();
        if (StringUtils.isNoneBlank(cssClass)) {
            return cssClass;
        }

        return DEFAULT_ICON;
    }

    public String getNumberLabel() {
        return numberLabel;
    }

    public String getMessage() {
        return message;
    }

    public String getStyleColor() {
        return styleColor;
    }

    public String getStyleCssStyle() {
        return styleCssStyle;
    }

    public String getIconCssClass() {
        return iconCssClass;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
