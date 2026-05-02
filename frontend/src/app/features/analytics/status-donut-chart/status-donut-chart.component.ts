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
import { PieChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsOption } from 'echarts';
import { StatusDistributionItem } from '../../../core/models/analytics.model';

echarts.use([PieChart, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer]);

/**
 * StatusDonutChartComponent
 *
 * Renders a donut chart showing the count and percentage of tasks in each
 * status (PENDING, COMPLETED, CANCELLED) using ngx-echarts.
 *
 * Req 9.3: Pie/donut chart for status distribution.
 * Req 13.1: WCAG 2.1 AA — aria-label on chart container + visually-hidden
 *           data table as an accessible alternative.
 */
@Component({
  selector: 'app-status-donut-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './status-donut-chart.component.html',
  styleUrl: './status-donut-chart.component.scss',
  imports: [CommonModule, NgxEchartsDirective],
  providers: [provideEchartsCore({ echarts })],
})
export class StatusDonutChartComponent {
  @Input({ required: true }) set data(value: StatusDistributionItem[]) {
    this._data.set(value);
  }

  protected readonly _data = signal<StatusDistributionItem[]>([]);

  /** Status colours — use both colour and label (WCAG 1.4.1). */
  private readonly STATUS_COLORS: Record<string, string> = {
    PENDING:   '#d97706', // Amber 600
    COMPLETED: '#16a34a', // Green 600
    CANCELLED: '#6b7280', // Gray 500
  };

  protected readonly chartOptions = computed<EChartsOption>(() => {
    const items = this._data();
    return {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)',
      },
      legend: {
        orient: 'horizontal',
        bottom: 0,
        itemGap: 16,
        textStyle: { color: '#111827', fontSize: 13 },
      },
      series: [
        {
          name: 'Task status',
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          label: {
            show: true,
            formatter: '{b}\n{d}%',
            fontSize: 12,
            color: '#111827',
          },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: 'bold' },
          },
          data: items.map((item) => ({
            name: this.formatStatus(item.status),
            value: item.count,
            itemStyle: {
              color: this.STATUS_COLORS[item.status] ?? '#9ca3af',
            },
          })),
        },
      ],
    };
  });

  /** Total task count for the accessible summary. */
  protected readonly total = computed(() =>
    this._data().reduce((sum, item) => sum + item.count, 0),
  );

  private formatStatus(status: string): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
  }
}
