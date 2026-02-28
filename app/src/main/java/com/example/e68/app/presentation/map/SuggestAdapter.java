package com.example.e68.app.presentation.map;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e68.app.R;

import java.util.function.Consumer;

/**
 * Адаптер списка саджест-подсказок поиска.
 */
public class SuggestAdapter extends ListAdapter<SuggestResultItem, SuggestAdapter.SuggestViewHolder> {

    private final Consumer<SuggestResultItem> onItemClick;

    public SuggestAdapter(Consumer<SuggestResultItem> onItemClick) {
        super(DIFF_CALLBACK);
        this.onItemClick = onItemClick;
    }

    private static final DiffUtil.ItemCallback<SuggestResultItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SuggestResultItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull SuggestResultItem a,
                                               @NonNull SuggestResultItem b) {
                    return a.title.equals(b.title);
                }

                @Override
                public boolean areContentsTheSame(@NonNull SuggestResultItem a,
                                                  @NonNull SuggestResultItem b) {
                    return a.title.equals(b.title) &&
                            ((a.subtitle == null && b.subtitle == null) ||
                                    (a.subtitle != null && a.subtitle.equals(b.subtitle)));
                }
            };

    @NonNull
    @Override
    public SuggestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggest, parent, false);
        return new SuggestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestViewHolder holder, int position) {
        holder.bind(getItem(position), onItemClick);
    }

    static class SuggestViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvSubtitle;

        SuggestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tvSuggestTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSuggestSubtitle);
        }

        void bind(SuggestResultItem item, Consumer<SuggestResultItem> onClick) {
            tvTitle.setText(item.title);
            if (item.subtitle != null && !item.subtitle.isEmpty()) {
                tvSubtitle.setVisibility(View.VISIBLE);
                tvSubtitle.setText(item.subtitle);
            } else {
                tvSubtitle.setVisibility(View.GONE);
            }
            itemView.setOnClickListener(v -> onClick.accept(item));
        }
    }
}