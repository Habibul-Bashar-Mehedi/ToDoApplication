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
import { PriorityAnalysisItem } from '../../../core/models/analytics.model';

echarts.use([BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer]);

/**
 * PriorityStackedBarChartComponent
 *
 * Renders a stacked bar chart showing the distribution of tasks by priority
 * (HIGH, MEDIUM, LOW) and the completion rate within each priority level.
 *
 * Req 9.6: Stacked bar chart — task count and completion rate per priority.
 * Req 13.1: WCAG 2.1 AA — aria-label + visually-hidden data table.
 */
@Component({
  selector: 'app-priority-stacked-bar-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './priority-stacked-bar-chart.component.html',
  styleUrl: './priority-stacked-bar-chart.component.scss',
  imports: [CommonModule, NgxEchartsDirective],
  providers: [provideEchartsCore({ echarts })],
})
export class PriorityStackedBarChartComponent {
  @Input({ required: true }) set data(value: PriorityAnalysisItem[]) {
    this._data.set(value);
  }

  protected readonly _data = signal<PriorityAnalysisItem[]>([]);

  private readonly PRIORITY_ORDER = ['HIGH', 'MEDIUM', 'LOW'];
  private readonly PRIORITY_COLORS: Record<string, string> = {
    HIGH:   '#dc2626', // Red 600
    MEDIUM: '#d97706', // Amber 600
    LOW:    '#16a34a', // Green 600
  };

  protected readonly chartOptions = computed<EChartsOption>(() => {
    // Sort by defined priority order
    const items = [...this._data()].sort(
      (a, b) =>
        this.PRIORITY_ORDER.indexOf(a.priority) -
        this.PRIORITY_ORDER.indexOf(b.priority),
    );

    const priorities = items.map((i) => this.formatPriority(i.priority));
    const counts = items.map((i) => i.count);
    const completionRates = items.map((i) => Math.round(i.completionRate * 100));

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: unknown) => {
          const ps = params as Array<{ seriesName: string; value: number; name: string }>;
          const priority = ps[0]?.name ?? '';
          return ps
            .map((p) =>
              p.seriesName === 'Completion rate'
                ? `${p.seriesName}: ${p.value}%`
                : `${p.seriesName}: ${p.value}`,
            )
            .join('<br/>')
            .replace(/^/, `<strong>${priority}</strong><br/>`);
        },
      },
      legend: {
        data: ['Task count', 'Completion rate (%)'],
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
        data: priorities,
        axisLabel: { fontSize: 12, color: '#111827' },
        axisLine: { lineStyle: { color: '#e5e7eb' } },
      },
      yAxis: [
        {
          type: 'value',
          name: 'Tasks',
          minInterval: 1,
          axisLabel: { fontSize: 11, color: '#6b7280' },
          splitLine: { lineStyle: { color: '#f3f4f6' } },
        },
        {
          type: 'value',
          name: 'Rate (%)',
          min: 0,
          max: 100,
          axisLabel: {
            formatter: '{value}%',
            fontSize: 11,
            color: '#6b7280',
          },
          splitLine: { show: false },
        },
      ],
      series: [
        {
          name: 'Task count',
          type: 'bar',
          yAxisIndex: 0,
          data: counts.map((count, idx) => ({
            value: count,
            itemStyle: {
              color: this.PRIORITY_COLORS[items[idx]?.priority] ?? '#9ca3af',
              borderRadius: [4, 4, 0, 0],
            },
          })),
          barMaxWidth: 48,
        },
        {
          name: 'Completion rate (%)',
          type: 'bar',
          yAxisIndex: 1,
          data: completionRates.map((rate) => ({
            value: rate,
            itemStyle: {
              color: 'rgba(79,70,229,0.7)',
              borderRadius: [4, 4, 0, 0],
            },
          })),
          barMaxWidth: 48,
          barGap: '30%',
          label: {
            show: true,
            position: 'top',
            formatter: '{c}%',
            fontSize: 11,
            color: '#4f46e5',
          },
        },
      ],
    };
  });

  private formatPriority(priority: string): string {
    return priority.charAt(0) + priority.slice(1).toLowerCase();
  }
}
