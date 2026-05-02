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
import { BarChart } from 'echarts/charts';
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsOption } from 'echarts';
import { ModulePerformanceItem } from '../../../core/models/analytics.model';

echarts.use([BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer]);

/**
 * ModulePerformanceBarChartComponent
 *
 * Renders a horizontal bar chart comparing the completion rate of each module
 * the user owns or is a member of.
 *
 * Req 9.5: Bar chart — completion rate per module.
 * Req 13.1: WCAG 2.1 AA — aria-label + visually-hidden data table.
 */
@Component({
  selector: 'app-module-performance-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './module-performance-chart.component.html',
  styleUrl: './module-performance-chart.component.scss',
  imports: [CommonModule, NgxEchartsDirective],
  providers: [provideEchartsCore({ echarts })],
})
export class ModulePerformanceChartComponent {
  @Input({ required: true }) set data(value: ModulePerformanceItem[]) {
    this._data.set(value);
  }

  protected readonly _data = signal<ModulePerformanceItem[]>([]);

  /** Dynamic chart height based on number of modules (min 200px). */
  protected readonly chartHeight = computed(() => {
    const count = this._data().length;
    return Math.max(200, count * 44 + 60);
  });

  protected readonly chartOptions = computed<EChartsOption>(() => {
    const items = this._data();
    const names = items.map((i) => i.moduleName);
    const rates = items.map((i) => Math.round(i.completionRate * 100));

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: unknown) => {
          const p = (params as Array<{ name: string; value: number }>)[0];
          return `${p.name}: ${p.value}%`;
        },
      },
      grid: {
        left: '3%',
        right: '8%',
        bottom: '3%',
        top: '3%',
        containLabel: true,
      },
      xAxis: {
        type: 'value',
        min: 0,
        max: 100,
        axisLabel: {
          formatter: '{value}%',
          fontSize: 11,
          color: '#6b7280',
        },
        splitLine: { lineStyle: { color: '#f3f4f6' } },
      },
      yAxis: {
        type: 'category',
        data: names,
        axisLabel: {
          fontSize: 12,
          color: '#111827',
          overflow: 'truncate',
          width: 120,
        },
        axisLine: { lineStyle: { color: '#e5e7eb' } },
      },
      series: [
        {
          name: 'Completion rate',
          type: 'bar',
          data: rates.map((rate) => ({
            value: rate,
            itemStyle: {
              color:
                rate >= 75
                  ? '#16a34a'
                  : rate >= 40
                    ? '#d97706'
                    : '#4f46e5',
              borderRadius: [0, 4, 4, 0],
            },
          })),
          label: {
            show: true,
            position: 'right',
            formatter: '{c}%',
            fontSize: 12,
            color: '#111827',
          },
          barMaxWidth: 32,
        },
      ],
    };
  });
}
