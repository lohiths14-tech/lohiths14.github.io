package com.smartfind.app.presentation.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smartfind.app.databinding.ItemOnboardingBinding

/**
 * ViewPager2 adapter for displaying onboarding screens.
 *
 * Each page shows:
 * - Feature illustration
 * - Title
 * - Description
 * - Custom background color
 *
 * @param items List of onboarding pages to display
 */
class OnboardingAdapter(
    private val items: List<OnboardingItem>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /**
     * ViewHolder for a single onboarding page
     */
    inner class OnboardingViewHolder(
        private val binding: ItemOnboardingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Bind onboarding item data to views
         */
        fun bind(item: OnboardingItem) {
            binding.apply {
                // Set image
                ivOnboarding.setImageResource(item.imageRes)

                // Set text
                tvTitle.text = item.title
                tvDescription.text = item.description

                // Set background color (optional, as parent can handle this)
                val color = ContextCompat.getColor(root.context, item.backgroundColor)
                root.setBackgroundColor(color)

                // Add animations for smooth appearance
                ivOnboarding.alpha = 0f
                tvTitle.alpha = 0f
                tvDescription.alpha = 0f

                ivOnboarding.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .setStartDelay(100)
                    .start()

                tvTitle.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .setStartDelay(200)
                    .start()

                tvDescription.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .setStartDelay(300)
                    .start()
            }
        }
    }
}
