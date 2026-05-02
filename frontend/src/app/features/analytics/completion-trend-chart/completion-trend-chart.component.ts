import {
  Component,
  Input,
  ChangeDetectionStrategy,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxEchartsDirective, provideEchartsCore } from 'ngx-echarts';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsOption } from 'echarts';
import { CompletionTrendItem } from '../../../core/models/analytics.model';

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer]);

/**
 * CompletionTrendLineChartComponent
 *
 * Renders a line chart showing tasks created vs tasks completed over the
 * selected date range, using ngx-echarts.
 *
 * Req 9.4: Line chart — created vs completed over time.
 * Req 13.1: WCAG 2.1 AA — aria-label + visually-hidden data table.
 */
@Component({
  selector: 'app-completion-trend-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './completion-trend-chart.component.html',
  styleUrl: './completion-trend-chart.component.scss',
  imports: [CommonModule, NgxEchartsDirective],
  providers: [provideEchartsCore({ echarts })],
})
export class CompletionTrendChartComponent {
  @Input({ required: true }) set data(value: CompletionTrendItem[]) {
    this._data.set(value);
  }

  protected readonly _data = signal<CompletionTrendItem[]>([]);

  protected readonly chartOptions = computed<EChartsOption>(() => {
    const items = this._data();
    const dates = items.map((i) => i.date);
    const created = items.map((i) => i.created);
    const completed = items.map((i) => i.completed);

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
      },
      legend: {
        data: ['Created', 'Completed'],
        bottom: 0,
        textStyle: { color: '#111827', fontSize: 13 },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '12%',
        top: '8%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: {
          rotate: dates.length > 14 ? 45 : 0,
          fontSize: 11,
          color: '#6b7280',
        },
        axisLine: { lineStyle: { color: '#e5e7eb' } },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { fontSize: 11, color: '#6b7280' },
        splitLine: { lineStyle: { color: '#f3f4f6' } },
      },
      series: [
        {
          name: 'Created',
          type: 'line',
          data: created,
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color: '#4f46e5', width: 2 },
          itemStyle: { color: '#4f46e5' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(79,70,229,0.15)' },
                { offset: 1, color: 'rgba(79,70,229,0)' },
              ],
            },
          },
        },
        {
          name: 'Completed',
          type: 'line',
          data: completed,
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { color: '#16a34a', width: 2 },
          itemStyle: { color: '#16a34a' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(22,163,74,0.15)' },
                { offset: 1, color: 'rgba(22,163,74,0)' },
              ],
            },
          },
        },
      ],
    };
  });
}
